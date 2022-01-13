package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
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
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;

@ExtendWith(MockitoExtension.class)
class CaseConfigurationProviderServiceTest {

    @Mock
    private CcdDataService ccdDataService;
    @Mock
    private DmnEvaluationService dmnEvaluationService;

    @Spy
    private ObjectMapper objectMapper;

    private CaseConfigurationProviderService caseConfigurationProviderService;

    @Mock
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        caseConfigurationProviderService = new CaseConfigurationProviderService(
            ccdDataService,
            dmnEvaluationService,
            objectMapper
        );

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));
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
}
