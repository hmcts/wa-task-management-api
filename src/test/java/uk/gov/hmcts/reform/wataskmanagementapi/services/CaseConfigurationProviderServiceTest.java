package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DueDateCalculator;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DueDateIntervalCalculator;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DueDateTimeCalculator;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.PublicHolidaysCollection;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.WorkingDayIndicator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;

@ExtendWith(MockitoExtension.class)
class CaseConfigurationProviderServiceTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);

    @Mock
    private CcdDataService ccdDataService;
    @Mock
    private DmnEvaluationService dmnEvaluationService;

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    @Spy
    private ObjectMapper objectMapper;

    private CaseConfigurationProviderService caseConfigurationProviderService;

    @Mock
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        caseConfigurationProviderService = new CaseConfigurationProviderService(
            ccdDataService,
            dmnEvaluationService,
            objectMapper,
            new DateTypeConfigurator(
                List.of(
                    new DueDateCalculator(),
                    new DueDateIntervalCalculator(new WorkingDayIndicator(publicHolidaysCollection)),
                    new DueDateTimeCalculator()
                ))
        );

        lenient().when(caseDetails.getCaseType()).thenReturn("Asylum");
        lenient().when(caseDetails.getJurisdiction()).thenReturn("IA");
        lenient().when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        Set<LocalDate> localDates = Set.of(
            LocalDate.of(2022, 1, 3),
            LocalDate.of(2022, 4, 15),
            LocalDate.of(2022, 4, 18),
            LocalDate.of(2022, 5, 2),
            LocalDate.of(2022, 6, 2),
            LocalDate.of(2022, 6, 3),
            LocalDate.of(2022, 8, 29),
            LocalDate.of(2022, 9, 19),
            LocalDate.of(2022, 12, 26),
            LocalDate.of(2022, 12, 27)
        );

        lenient().when(publicHolidaysCollection.getPublicHolidays(List.of(CALENDAR_URI))).thenReturn(localDates);
    }

    public static Stream<Arguments> scenarioProvider() {
        return Stream.of(
            Arguments.of(Map.of("taskType", "some task id"), "{\"taskType\":\"some task id\"}"),
            Arguments.of(Map.of("taskType", ""), "{\"taskType\":\"\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("scenarioProvider")
    void given_r2_feature_flag_value_when_evaluate_configuration_dmn_then_taskDetails_is_as_expected(
        Map<String, Object> inputTaskAttributes,
        String expectedTaskAttributes) {

        when(ccdDataService.getCaseData("some case id")).thenReturn(caseDetails);

        caseConfigurationProviderService.getCaseRelatedConfiguration("some case id", inputTaskAttributes, false);

        verify(dmnEvaluationService).evaluateTaskConfigurationDmn(
            "IA",
            "Asylum",
            "{}",
            expectedTaskAttributes
        );

        verify(dmnEvaluationService).evaluateTaskPermissionsDmn(
            "IA",
            "Asylum",
            "{}",
            expectedTaskAttributes
        );
    }

    @Test
    void does_not_have_any_fields_to_map() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(dmnEvaluationService.evaluateTaskPermissionsDmn("IA", "Asylum", "{}", "{}"))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunalCaseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("seniorTribunalCaseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            ));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.now().plusDays(2);
        String defaultDate = date.format(formatter) + "T16:00";

        Map<String, Object> expectedMappedData = new HashMap<>();
        expectedMappedData.put("tribunalCaseworker", "Read,Refer,Own,Manage,Cancel");
        expectedMappedData.put("seniorTribunalCaseworker", "Read,Refer,Own,Manage,Cancel");
        expectedMappedData.put("securityClassification", "PUBLIC");
        expectedMappedData.put("jurisdiction", "IA");
        expectedMappedData.put("caseTypeId", "Asylum");
        expectedMappedData.put("dueDate", defaultDate);
        expectedMappedData.put("priorityDate", defaultDate);

        Map<String, Object> taskAttributes = Map.of();
        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, taskAttributes, false);

        assertThat(mappedData.getProcessVariables(), is(expectedMappedData));
    }

    @Test
    void gets_fields_to_map() {
        String someCaseId = "someCaseId";
        String taskAttributesString = "{\"taskType\":\"taskType\"}";
        Map<String, Object> taskAttributes = Map.of("taskType", "taskType");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.now().plusDays(2);
        String defaultDate = date.format(formatter) + "T16:00";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(dmnEvaluationService.evaluateTaskConfigurationDmn("IA", "Asylum", "{}", taskAttributesString))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("name1"), stringValue("value1")),
                new ConfigurationDmnEvaluationResponse(stringValue("name2"), stringValue("value2"))
            ));

        Map<String, Object> expectedMappedData = Map.of(
            "name1", "value1",
            "name2", "value2",
            "securityClassification", "PUBLIC",
            "jurisdiction", "IA",
            "caseTypeId", "Asylum",
            "dueDate", defaultDate,
            "priorityDate", defaultDate
        );

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, taskAttributes, false);

        assertThat(mappedData.getProcessVariables(), is(expectedMappedData));

    }

    @Test
    void should_consider_permissions_when_case_access_category_column_matches_with_different_sets() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(caseDetails.getData()).thenReturn(Map.of("caseAccessCategory", "categoryA,categoryB"));
        List<PermissionsDmnEvaluationResponse> permissions = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA,categoryC")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB,categoryD")
            )
        );

        String caseData = "{\"caseAccessCategory\":\"categoryA,categoryB\"}";
        when(dmnEvaluationService.evaluateTaskPermissionsDmn("IA", "Asylum", caseData, "{}"))
            .thenReturn(permissions);


        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), false);

        assertThat(mappedData.getPermissionsDmnResponse(), is(permissions));
    }

    @Test
    void should_consider_permissions_when_case_access_category_column_matches() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(caseDetails.getData()).thenReturn(Map.of("caseAccessCategory", "categoryA,categoryB"));
        List<PermissionsDmnEvaluationResponse> permissions = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB")
            )
        );

        String caseData = "{\"caseAccessCategory\":\"categoryA,categoryB\"}";
        when(dmnEvaluationService.evaluateTaskPermissionsDmn("IA", "Asylum", caseData, "{}"))
            .thenReturn(permissions);


        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), false);

        assertThat(mappedData.getPermissionsDmnResponse(), is(permissions));
    }

    @Test
    void should_consider_permissions_when_case_access_category_column_is_empty() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(caseDetails.getData()).thenReturn(Map.of("caseAccessCategory", "categoryA"));
        List<PermissionsDmnEvaluationResponse> permissions = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                null
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("")
            )
        );

        String caseData = "{\"caseAccessCategory\":\"categoryA\"}";
        when(dmnEvaluationService.evaluateTaskPermissionsDmn("IA", "Asylum", caseData, "{}"))
            .thenReturn(permissions);


        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), false);

        assertThat(mappedData.getPermissionsDmnResponse(), is(permissions));
    }

    @Test
    void should_return_empty_permissions_when_case_access_category_column_does_not_matches() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(caseDetails.getData()).thenReturn(Map.of("caseAccessCategory", "categoryA"));
        List<PermissionsDmnEvaluationResponse> permissions = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryC")
            )
        );

        String caseData = "{\"caseAccessCategory\":\"categoryA\"}";
        when(dmnEvaluationService.evaluateTaskPermissionsDmn("IA", "Asylum", caseData, "{}"))
            .thenReturn(permissions);


        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), false);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
    }

    @Test
    void should_return_empty_permissions_when_case_access_category_column_is_present_but_case_field_is_null() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        List<PermissionsDmnEvaluationResponse> permissions = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryC")
            )
        );

        when(dmnEvaluationService.evaluateTaskPermissionsDmn("IA", "Asylum", "{}", "{}"))
            .thenReturn(permissions);


        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), false);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
    }

    @Test
    void should_return_empty_permissions_when_case_access_category_column_is_present_but_case_field_is_empty() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(caseDetails.getData()).thenReturn(Map.of("caseAccessCategory", ""));

        List<PermissionsDmnEvaluationResponse> permissions = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryC")
            )
        );

        String caseData = "{\"caseAccessCategory\":\"\"}";

        when(dmnEvaluationService.evaluateTaskPermissionsDmn("IA", "Asylum", caseData, "{}"))
            .thenReturn(permissions);

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), false);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
    }

    @Test
    void should_include_additional_properties_when_task_configuration_dmn_returns_individual_additional_properties() {
        String someCaseId = "someCaseId";
        String taskAttributesString = "{\"taskType\":\"taskType\"}";
        Map<String, Object> taskAttributes = Map.of("taskType", "taskType");

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(caseDetails.getData()).thenReturn(Map.of("caseAccessCategory", ""));

        List<PermissionsDmnEvaluationResponse> permissions = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryC")
            )
        );

        String caseData = "{\"caseAccessCategory\":\"\"}";

        when(dmnEvaluationService.evaluateTaskPermissionsDmn("IA", "Asylum", caseData, taskAttributesString))
            .thenReturn(permissions);

        when(dmnEvaluationService.evaluateTaskConfigurationDmn("IA", "Asylum", caseData, taskAttributesString))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("name1"), stringValue("value1")),
                new ConfigurationDmnEvaluationResponse(stringValue("name2"), stringValue("value2")),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties_name3"),
                    stringValue("value3")
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties_name4"),
                    stringValue("value4")
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties_name5"),
                    stringValue("value5")
                ),
                new ConfigurationDmnEvaluationResponse(stringValue("additionalProperties_name6"), stringValue("value6"))
            ));

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, taskAttributes, false);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Map<String, String> additionalProperties = ImmutableMap.of(
            "name6",
            "value6",
            "name5",
            "value5",
            "name4",
            "value4",
            "name3",
            "value3"
        );
        Assertions.assertThat(mappedData.getConfigurationDmnResponse()).isNotEmpty()
            .hasSize(5)
            .contains(
                new ConfigurationDmnEvaluationResponse(stringValue("name1"), stringValue("value1")),
                new ConfigurationDmnEvaluationResponse(stringValue("name2"), stringValue("value2")),
                new ConfigurationDmnEvaluationResponse(stringValue("additionalProperties"),
                                                       stringValue(writeValueAsString(additionalProperties))));
    }

    @Test
    void should_replace_with_sent_additional_properties() {
        String someCaseId = "someCaseId";
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, Object> taskAttributes = Map.of(
            "taskAdditionalProperties",
            Map.of("roleAssignmentId", roleAssignmentId)
        );
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        List<PermissionsDmnEvaluationResponse> permissions = List.of(
            new PermissionsDmnEvaluationResponse(
                stringValue("reviewSpecificAccessRequestJudiciary"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("JUDICIAL"),
                stringValue("categoryB")
            )
        );

        lenient().when(dmnEvaluationService.evaluateTaskPermissionsDmn(any(), any(), any(), any()))
            .thenReturn(permissions);

        lenient().when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties_roleAssignmentId"),
                    stringValue(roleAssignmentId)
                )
            ));

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, taskAttributes, false);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Map<String, String> additionalProperties = ImmutableMap.of(
            "roleAssignmentId", roleAssignmentId
        );
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
            .filteredOn(r -> r.getName().getValue().equals("additionalProperties"))
            .isNotEmpty()
            .hasSize(1)
            .contains(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties"),
                    stringValue(writeValueAsString(additionalProperties))
                ));
    }

    @Test
    void should_replace_sent_additional_properties_when_configuration_dmn_contains() {
        String someCaseId = "someCaseId";
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, Object> taskAttributes = Map.of(
            "taskAdditionalProperties",
            Map.of(
                "roleAssignmentId", roleAssignmentId,
                "key", "nonExistValue"
            )
        );
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);

        List<PermissionsDmnEvaluationResponse> permissions = List.of(
            new PermissionsDmnEvaluationResponse(
                stringValue("reviewSpecificAccessRequestJudiciary"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("JUDICIAL"),
                stringValue("categoryB")
            )
        );

        lenient().when(dmnEvaluationService.evaluateTaskPermissionsDmn(any(), any(), any(), any()))
            .thenReturn(permissions);

        lenient().when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties_roleAssignmentId"),
                    stringValue(roleAssignmentId)
                )
            ));

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, taskAttributes, false);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Map<String, String> additionalProperties = ImmutableMap.of(
            "roleAssignmentId", roleAssignmentId
        );
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
            .filteredOn(r -> r.getName().getValue().equals("additionalProperties"))
            .isNotEmpty()
            .hasSize(1)
            .contains(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties"),
                    stringValue(writeValueAsString(additionalProperties))
                ));
    }

    @Test
    void should_return_default_value_from_configuration_dmn_when_additional_properties_is_empty() {
        String someCaseId = "someCaseId";
        Map<String, Object> taskAttributes = Map.of("taskAdditionalProperties", Map.of());
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);

        List<PermissionsDmnEvaluationResponse> permissions = List.of(
            new PermissionsDmnEvaluationResponse(
                stringValue("reviewSpecificAccessRequestJudiciary"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("JUDICIAL"),
                stringValue("categoryB")
            )
        );

        lenient().when(dmnEvaluationService.evaluateTaskPermissionsDmn(any(), any(), any(), any()))
            .thenReturn(permissions);

        lenient().when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties_roleAssignmentId"),
                    stringValue("roleAssignmentId")
                )
            ));

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, taskAttributes, false);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Map<String, String> additionalProperties = ImmutableMap.of(
            "roleAssignmentId", "roleAssignmentId"
        );
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
            .filteredOn(r -> r.getName().getValue().equals("additionalProperties"))
            .isNotEmpty()
            .hasSize(1)
            .contains(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties"),
                    stringValue(writeValueAsString(additionalProperties))
                ));
    }

    @Test
    void should_evaluate_task_configuration_and_return_dmn_results() {
        String someCaseId = "someCaseId";
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        lenient().when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties_roleAssignmentId"),
                    stringValue("roleAssignmentId"),
                    booleanValue(true)
                )
            ));

        List<ConfigurationDmnEvaluationResponse> results = caseConfigurationProviderService
            .evaluateConfigurationDmn(someCaseId, null);

        Assertions.assertThat(results)
            .isNotEmpty()
            .hasSize(1)
            .contains(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties_roleAssignmentId"),
                    stringValue("roleAssignmentId"),
                    booleanValue(true)
                ));
    }

    @Test
    void should_evaluate_task_configuration_dmn_and_return_empty_dmn_results() {
        String someCaseId = "someCaseId";
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        lenient().when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of());

        List<ConfigurationDmnEvaluationResponse> results = caseConfigurationProviderService
            .evaluateConfigurationDmn(someCaseId, null);

        Assertions.assertThat(results)
            .isEmpty();
    }

    @Test
    void should_evaluate_task_configuration_dmn_when_json_mapping_exception_return_empty_results() throws Exception {
        String someCaseId = "someCaseId";
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(objectMapper.writeValueAsString(anyMap())).thenThrow(JsonProcessingException.class);
        lenient().when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of());

        List<ConfigurationDmnEvaluationResponse> results = caseConfigurationProviderService
            .evaluateConfigurationDmn(someCaseId, null);

        Assertions.assertThat(results)
            .isEmpty();
    }

    @Test
    void should_calculate_due_date_from_given_due_date_properties() {
        String someCaseId = "someCaseId";
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);

        lenient().when(dmnEvaluationService.evaluateTaskPermissionsDmn(any(), any(), any(), any()))
            .thenReturn(List.of());

        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(stringValue("dueDate"), stringValue(localDateTime)),
                new ConfigurationDmnEvaluationResponse(stringValue("dueDateTime"), stringValue("18:00"))
            ));

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), false);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .isNotEmpty()
            .hasSize(1)
            .contains(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDate"),
                    stringValue(localDateTime + "T18:00")
                ));
    }

    @Test
    void should_calculate_due_date_from_initiation_due_date_if_present() {
        String someCaseId = "someCaseId";
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);

        lenient().when(dmnEvaluationService.evaluateTaskPermissionsDmn(any(), any(), any(), any()))
            .thenReturn(List.of());

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of("dueDate", "2022-12-13T13:08:00.170Z"), false);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .isEmpty();
    }

    @Test
    void should_recalculate_due_date_from_given_due_date_properties() {
        String someCaseId = "someCaseId";
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);

        lenient().when(dmnEvaluationService.evaluateTaskPermissionsDmn(any(), any(), any(), any()))
            .thenReturn(List.of());

        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDate"),
                    stringValue(localDateTime),
                    booleanValue(true)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateTime"),
                    stringValue("18:00"),
                    booleanValue(true)
                )
            ));

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), true);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
            .isNotEmpty()
            .hasSize(1)
            .contains(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDate"),
                    stringValue(localDateTime + "T18:00")
                ));
    }

    @Test
    void should_not_recalculate_due_date_from_given_due_date_properties() {
        String someCaseId = "someCaseId";
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);

        lenient().when(dmnEvaluationService.evaluateTaskPermissionsDmn(any(), any(), any(), any()))
            .thenReturn(List.of());

        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDate"),
                    stringValue(localDateTime),
                    booleanValue(false)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateTime"),
                    stringValue("18:00"),
                    booleanValue(false)
                )
            ));

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), true);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Assertions.assertThat(mappedData.getConfigurationDmnResponse()).isEmpty();
    }

    @Test
    void should_calculate_due_date_interval_from_given_due_date_origin_properties() {
        String someCaseId = "someCaseId";
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);

        lenient().when(dmnEvaluationService.evaluateTaskPermissionsDmn(any(), any(), any(), any()))
            .thenReturn(List.of());

        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateOrigin"),
                    stringValue(localDateTime + "T20:00")
                ),
                new ConfigurationDmnEvaluationResponse(stringValue("dueDateIntervalDays"), stringValue("6")),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateNonWorkingCalendar"),
                    stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json")
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateNonWorkingDaysOfWeek"),
                    stringValue("SATURDAY, SUNDAY")
                ),
                new ConfigurationDmnEvaluationResponse(stringValue("dueDateSkipNonWorkingDays"), stringValue("true")),
                new ConfigurationDmnEvaluationResponse(stringValue("dueDateMustBeWorkingDay"), stringValue("Next")),
                new ConfigurationDmnEvaluationResponse(stringValue("dueDateTime"), stringValue("18:00"))
            ));

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), false);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        String expectedDate = GIVEN_DATE.plusDays(8).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .isNotEmpty()
            .hasSize(1)
            .contains(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDate"),
                    stringValue(expectedDate + "T18:00")
                ));
    }

    @Test
    void should_recalculate_due_date_interval_from_given_due_date_origin_properties() {
        String someCaseId = "someCaseId";
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);

        lenient().when(dmnEvaluationService.evaluateTaskPermissionsDmn(any(), any(), any(), any()))
            .thenReturn(List.of());

        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateOrigin"),
                    stringValue(localDateTime + "T20:00"),
                    booleanValue(true)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateIntervalDays"),
                    stringValue("6"),
                    booleanValue(true)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateNonWorkingCalendar"),
                    stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"),
                    booleanValue(true)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateNonWorkingDaysOfWeek"),
                    stringValue("SATURDAY, SUNDAY"),
                    booleanValue(true)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateSkipNonWorkingDays"),
                    stringValue("true"),
                    booleanValue(true)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateMustBeWorkingDay"),
                    stringValue("true"),
                    booleanValue(true)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateTime"),
                    stringValue("18:00"),
                    booleanValue(true)
                )
            ));

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), true);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        String expectedDate = GIVEN_DATE.plusDays(8).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
            .isNotEmpty()
            .hasSize(1)
            .contains(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDate"),
                    stringValue(expectedDate + "T18:00")
                ));
    }

    @Test
    void should_not_recalculate_due_date_interval_from_given_due_date_origin_properties() {
        String someCaseId = "someCaseId";
        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);

        lenient().when(dmnEvaluationService.evaluateTaskPermissionsDmn(any(), any(), any(), any()))
            .thenReturn(List.of());

        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateOrigin"),
                    stringValue(localDateTime + "T20:00"),
                    booleanValue(false)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateIntervalDays"),
                    stringValue("6"),
                    booleanValue(false)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateNonWorkingCalendar"),
                    stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"),
                    booleanValue(false)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateNonWorkingDaysOfWeek"),
                    stringValue("SATURDAY, SUNDAY"),
                    booleanValue(false)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateSkipNonWorkingDays"),
                    stringValue("true"),
                    booleanValue(false)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateMustBeWorkingDay"),
                    stringValue("true"),
                    booleanValue(false)
                ),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("dueDateTime"),
                    stringValue("18:00"),
                    booleanValue(false)
                )
            ));

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, Map.of(), true);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        String expectedDate = GIVEN_DATE.plusDays(8).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Assertions.assertThat(mappedData.getConfigurationDmnResponse()).isEmpty();
    }

    private String writeValueAsString(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            //do nothing
        }
        return null;
    }
}
