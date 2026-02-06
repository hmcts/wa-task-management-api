package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskCompleteException;

@Slf4j
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
}
