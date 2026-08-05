package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CompleteTaskVariables;

import java.util.Map;

@Service
public class CamundaRetryService {

    private final CamundaServiceApi camundaServiceApi;
    private final AuthTokenGenerator authTokenGenerator;

    public CamundaRetryService(CamundaServiceApi camundaServiceApi, AuthTokenGenerator authTokenGenerator) {
        this.camundaServiceApi = camundaServiceApi;
        this.authTokenGenerator = authTokenGenerator;
    }

    @Retryable(retryFor = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void completeTaskWithRetry(String taskId) {
        camundaServiceApi.completeTask(authTokenGenerator.generate(), taskId, new CompleteTaskVariables());
    }

    @Retryable(retryFor = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void claimTaskWithRetry(String taskId, Map<String, String> body) {
        camundaServiceApi.claimTask(authTokenGenerator.generate(), taskId, body);
    }

    @Retryable(retryFor = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void unclaimTaskWithRetry(String taskId) {
        camundaServiceApi.unclaimTask(authTokenGenerator.generate(), taskId);
    }

    @Retryable(retryFor = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void assignTaskWithRetry(String taskId, Map<String, String> body) {
        camundaServiceApi.assignTask(authTokenGenerator.generate(), taskId, body);
    }

    @Retryable(retryFor = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void addLocalVariablesToTaskWithRetry(String taskId, AddLocalVariableRequest addLocalVariableRequest) {
        camundaServiceApi.addLocalVariablesToTask(authTokenGenerator.generate(), taskId, addLocalVariableRequest);
    }

    @Retryable(retryFor = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void bpmnEscalationWithRetry(String taskId, Map<String, String> body) {
        camundaServiceApi.bpmnEscalation(authTokenGenerator.generate(), taskId, body);
    }
}
