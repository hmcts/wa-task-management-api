package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;

@ExtendWith(MockitoExtension.class)
class CaseConfigurationProviderServiceTest {

    @Mock
    private CcdDataService ccdDataService;
    @Mock
    private DmnEvaluationService dmnEvaluationService;
    @Mock
    private AuthTokenGenerator authTokenGenerator;

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

    @Test
    void does_not_have_any_fields_to_map() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            "IA",
            "Asylum",
            "{}",
            null
        ))
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
            null
        );

        assertThat(mappedData.getProcessVariables(), is(expectedMappedData));
    }

    @Test
    void gets_fields_to_map() {
        String someCaseId = "someCaseId";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(caseDetails);
        when(dmnEvaluationService.evaluateTaskConfigurationDmn("IA", "Asylum", "{}"))
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

        TaskConfigurationResults mappedData = caseConfigurationProviderService.getCaseRelatedConfiguration(
            someCaseId,
            null
        );

        assertThat(mappedData.getProcessVariables(), is(expectedMappedData));

    }
}
