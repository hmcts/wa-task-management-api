package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.BankHolidaysApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.DueDateCalculator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.DueDateOriginBasedCalculator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.PublicHolidaysCollection;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.WorkingDayIndicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;

@ExtendWith(MockitoExtension.class)
class CaseConfigurationProviderServiceTest {

    @Mock
    private CcdDataService ccdDataService;
    @Mock
    private DmnEvaluationService dmnEvaluationService;

    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private BankHolidaysApi bankHolidaysApi;

    private CaseConfigurationProviderService caseConfigurationProviderService;

    @Mock
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        caseConfigurationProviderService = new CaseConfigurationProviderService(
            ccdDataService,
            dmnEvaluationService,
            objectMapper,
            new DueDateCalculator(new DueDateOriginBasedCalculator(
                new WorkingDayIndicator(new PublicHolidaysCollection())))
        );

        lenient().when(caseDetails.getCaseType()).thenReturn("Asylum");
        lenient().when(caseDetails.getJurisdiction()).thenReturn("IA");
        lenient().when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));
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

        caseConfigurationProviderService.getCaseRelatedConfiguration("some case id", inputTaskAttributes);

        verify(dmnEvaluationService).evaluateTaskConfigurationDmn(
            eq("IA"),
            eq("Asylum"),
            eq("{}"),
            eq(expectedTaskAttributes)
        );

        verify(dmnEvaluationService).evaluateTaskPermissionsDmn(
            eq("IA"),
            eq("Asylum"),
            eq("{}"),
            eq(expectedTaskAttributes)
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

        Map<String, Object> expectedMappedData = new HashMap<>();
        expectedMappedData.put("tribunalCaseworker", "Read,Refer,Own,Manage,Cancel");
        expectedMappedData.put("seniorTribunalCaseworker", "Read,Refer,Own,Manage,Cancel");
        expectedMappedData.put("securityClassification", "PUBLIC");
        expectedMappedData.put("jurisdiction", "IA");
        expectedMappedData.put("caseTypeId", "Asylum");

        Map<String, Object> taskAttributes = Map.of();
        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, taskAttributes);

        assertThat(mappedData.getProcessVariables(), is(expectedMappedData));
    }

    @Test
    void gets_fields_to_map() {
        String someCaseId = "someCaseId";
        String taskAttributesString = "{\"taskType\":\"taskType\"}";
        Map<String, Object> taskAttributes = Map.of("taskType", "taskType");

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
            "caseTypeId", "Asylum"
        );

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, taskAttributes);

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
            .getCaseRelatedConfiguration(someCaseId, Map.of());

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
            .getCaseRelatedConfiguration(someCaseId, Map.of());

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
            .getCaseRelatedConfiguration(someCaseId, Map.of());

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
            .getCaseRelatedConfiguration(someCaseId, Map.of());

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
            .getCaseRelatedConfiguration(someCaseId, Map.of());

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
            .getCaseRelatedConfiguration(someCaseId, Map.of());

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
            .getCaseRelatedConfiguration(someCaseId, taskAttributes);

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
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
            .isNotEmpty()
            .hasSize(3)
            .contains(
                new ConfigurationDmnEvaluationResponse(stringValue("name1"), stringValue("value1")),
                new ConfigurationDmnEvaluationResponse(stringValue("name2"), stringValue("value2")),
                new ConfigurationDmnEvaluationResponse(
                    stringValue("additionalProperties"),
                    stringValue(writeValueAsString(additionalProperties))
                )
            );
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

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            new ConfigurationDmnEvaluationResponse(
                stringValue("additionalProperties_roleAssignmentId"),
                stringValue(roleAssignmentId)
            )
        );
        lenient().when(dmnEvaluationService.evaluateTaskConfigurationDmn(any(), any(), any(), any()))
            .thenReturn(evaluationResponses);

        TaskConfigurationResults mappedData = caseConfigurationProviderService
            .getCaseRelatedConfiguration(someCaseId, taskAttributes);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Map<String, String> additionalProperties = ImmutableMap.of(
            "roleAssignmentId", roleAssignmentId
        );
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
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
            .getCaseRelatedConfiguration(someCaseId, taskAttributes);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Map<String, String> additionalProperties = ImmutableMap.of(
            "roleAssignmentId", roleAssignmentId
        );
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
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
            .getCaseRelatedConfiguration(someCaseId, taskAttributes);

        Assertions.assertThat(mappedData.getPermissionsDmnResponse()).isEmpty();
        Map<String, String> additionalProperties = ImmutableMap.of(
            "roleAssignmentId", "roleAssignmentId"
        );
        Assertions.assertThat(mappedData.getConfigurationDmnResponse())
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


    private String writeValueAsString(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            //do nothing
        }
        return null;
    }
}
