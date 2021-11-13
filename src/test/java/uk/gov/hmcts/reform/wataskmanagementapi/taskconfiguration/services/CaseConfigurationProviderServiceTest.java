package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;

@ExtendWith(MockitoExtension.class)
class CaseConfigurationProviderServiceTest {

    @Mock
    private CcdDataService ccdDataService;
    @Mock
    private DmnEvaluationService dmnEvaluationService;

    @Mock
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

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
            objectMapper,
            featureFlagProvider);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        lenient().when(featureFlagProvider.getBooleanValue(eq(FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE), any(), any()))
            .thenReturn(true);
    }

    public static Stream<Arguments> scenarioProvider() {
        return Stream.of(
            Arguments.of(true, "some task id"),
            Arguments.of(false, "")
        );
    }

    @ParameterizedTest
    @MethodSource("scenarioProvider")
    void given_r2_feature_flag_value_when_evaluate_configuration_dmn_then_taskTypeId_is_as_expected(
        boolean featureFlag,
        String expectedTaskTypeId) {
        when(featureFlagProvider.getBooleanValue(eq(FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE), any(), any()))
            .thenReturn(featureFlag);
        when(ccdDataService.getCaseData("some case id")).thenReturn(caseDetails);

        caseConfigurationProviderService.getCaseRelatedConfiguration("some case id", expectedTaskTypeId);

        verify(dmnEvaluationService).evaluateTaskConfigurationDmn(eq("IA"),
            eq("Asylum"),
            eq("{}"),
            eq(expectedTaskTypeId));
    }

    @Test
    void does_not_have_any_fields_to_map() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(dmnEvaluationService.evaluateTaskPermissionsDmn("IA", "Asylum", "{}"))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunalCaseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS")
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("seniorTribunalCaseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS")
                )
            ));

        Map<String, Object> expectedMappedData = new HashMap<>();
        expectedMappedData.put("tribunalCaseworker", "Read,Refer,Own,Manage,Cancel");
        expectedMappedData.put("seniorTribunalCaseworker", "Read,Refer,Own,Manage,Cancel");
        expectedMappedData.put("securityClassification", "PUBLIC");
        expectedMappedData.put("jurisdiction", "IA");
        expectedMappedData.put("caseTypeId", "Asylum");
        TaskConfigurationResults mappedData = caseConfigurationProviderService.getCaseRelatedConfiguration(
            someCaseId,
            "some task type id"
        );
        assertThat(mappedData.getProcessVariables(), is(expectedMappedData));
    }

    @Test
    void gets_fields_to_map() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            "IA",
            "Asylum",
            "{}",
            "some task type id"
        )).thenReturn(asList(
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

        TaskConfigurationResults mappedData = caseConfigurationProviderService.getCaseRelatedConfiguration(
            someCaseId,
            "some task type id"
        );

        assertThat(mappedData.getProcessVariables(), is(expectedMappedData));

    }
}
