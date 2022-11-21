package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DmnRequest;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SpringBootTest
@ActiveProfiles({"integration"})
public class DmnEvaluationServiceTest {

    @MockBean
    private AuthTokenGenerator serviceAuthTokenGenerator;

    @MockBean
    private CamundaServiceApi camundaServiceApi;

    @Autowired
    private DmnEvaluationService dmnEvaluationService;

    @Test
    void should_call_camunda_api_once_when_retrieving_task_type_dmn() {
        String dmnName = "Task Types DMN";

        when(serviceAuthTokenGenerator.generate())
            .thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        dmnEvaluationService.retrieveTaskTypesDmn("wa", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("wa", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("wa", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("wa", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnName);

        verify(camundaServiceApi, times(1))
            .getTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                "wa",
                dmnName
            );

        verify(camundaServiceApi, times(1))
            .getTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                "ia",
                dmnName
            );

    }

    @Test
    void should_call_camunda_api_once_when_evaluating_task_type_dmn() {

        String dmnKey = "wa-task-types-wa-wacasetype";

        when(serviceAuthTokenGenerator.generate())
            .thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        dmnEvaluationService.evaluateTaskTypesDmn("wa", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("wa", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("wa", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("wa", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("ia", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("ia", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("ia", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("ia", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("ia", dmnKey);

        verify(camundaServiceApi, times(1))
            .evaluateTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                dmnKey,
                "wa",
                new DmnRequest<>()
            );

        verify(camundaServiceApi, times(1))
            .evaluateTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                dmnKey,
                "ia",
                new DmnRequest<>()
            );

    }

}
