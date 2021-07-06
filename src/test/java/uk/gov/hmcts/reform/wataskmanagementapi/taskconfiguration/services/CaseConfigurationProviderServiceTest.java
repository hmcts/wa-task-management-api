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
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.DecisionTableResult;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @BeforeEach
    void setUp() {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        caseConfigurationProviderService = new CaseConfigurationProviderService(
            ccdDataService,
            dmnEvaluationService,
            objectMapper);
    }

    @Test
    void does_not_have_any_fields_to_map() {
        String someCaseId = "someCaseId";
        String ccdData = "{"
                         + "\"jurisdiction\": \"IA\","
                         + "\"case_type\": \"Asylum\","
                         + "\"security_classification\": \"PUBLIC\","
                         + "\"data\": {}"
                         + "}";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(ccdData);
        when(dmnEvaluationService.evaluateTaskPermissionsDmn("IA", "Asylum", "{}"))
            .thenReturn(asList(
                new DecisionTableResult(
                    stringValue("tribunalCaseworker"), stringValue("Read,Refer,Own,Manage,Cancel")),
                new DecisionTableResult(
                    stringValue("seniorTribunalCaseworker"), stringValue("Read,Refer,Own,Manage,Cancel"))
            ));

        Map<String, Object> expectedMappedData = new HashMap<>();
        expectedMappedData.put("tribunalCaseworker", "Read,Refer,Own,Manage,Cancel");
        expectedMappedData.put("seniorTribunalCaseworker", "Read,Refer,Own,Manage,Cancel");
        expectedMappedData.put("securityClassification", "PUBLIC");
        expectedMappedData.put("jurisdiction", "IA");
        expectedMappedData.put("caseTypeId", "Asylum");
        Map<String, Object> mappedData = caseConfigurationProviderService.getCaseRelatedConfiguration(someCaseId);

        assertThat(mappedData, is(expectedMappedData));
    }

    @Test
    void cannot_parse_response_from_ccd() {
        assertThrows(
            IllegalStateException.class,
            () -> {
                String someCaseId = "someCaseId";
                String ccdData = "not valid json";
                when(ccdDataService.getCaseData(someCaseId)).thenReturn(ccdData);

                Map<String, Object> mappedData =
                    caseConfigurationProviderService.getCaseRelatedConfiguration(someCaseId);

                assertThat(mappedData, is(emptyMap()));
            }
        );
    }

    @Test
    void gets_fields_to_map() {
        String someCaseId = "someCaseId";
        String ccdData = "{ "
                         + "\"jurisdiction\": \"IA\","
                         + "\"case_type\": \"Asylum\","
                         + "\"security_classification\": \"PUBLIC\","
                         + "\"data\": {}"
                         + "}";

        when(ccdDataService.getCaseData(someCaseId)).thenReturn(ccdData);
        when(dmnEvaluationService.evaluateTaskConfigurationDmn("IA", "Asylum", "{}"))
            .thenReturn(asList(
                new DecisionTableResult(stringValue("name1"), stringValue("value1")),
                new DecisionTableResult(stringValue("name2"), stringValue("value2"))
            ));

        Map<String, Object> expectedMappedData = Map.of(
            "name1", "value1",
            "name2", "value2",
            "securityClassification", "PUBLIC",
            "jurisdiction", "IA",
            "caseTypeId", "Asylum");

        Map<String, Object> mappedData = caseConfigurationProviderService.getCaseRelatedConfiguration(someCaseId);

        assertThat(mappedData, is(expectedMappedData));

    }
}
