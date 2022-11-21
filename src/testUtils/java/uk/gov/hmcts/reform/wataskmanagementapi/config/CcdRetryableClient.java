package uk.gov.hmcts.reform.wataskmanagementapi.config;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;

@Slf4j
@Service
public class CcdRetryableClient {
    private final CoreCaseDataApi coreCaseDataApi;

    public CcdRetryableClient(CoreCaseDataApi coreCaseDataApi) {
        this.coreCaseDataApi = coreCaseDataApi;
    }

    @Retryable(value = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public StartEventResponse startForCaseworker(String authorisation, String serviceAuthorization,
        String userId, String jurisdictionId, String caseType, String eventId) {
        log.info("Calling submitEventForCaseWorker");
        return coreCaseDataApi.startForCaseworker(authorisation, serviceAuthorization,
                                                  userId, jurisdictionId, caseType, eventId);
    }

    @Retryable(value = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public CaseDetails submitForCaseworker(String authorisation, String serviceAuthorisation, String userId,
        String jurisdictionId, String caseType, boolean ignoreWarning, CaseDataContent caseDataContent) {
        log.info("Calling submitForCaseworker");
        return coreCaseDataApi.submitForCaseworker(authorisation, serviceAuthorisation, userId, jurisdictionId,
            caseType, ignoreWarning, caseDataContent);
    }



    @Retryable(value = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public StartEventResponse startEventForCaseWorker(String authorisation, String serviceAuthorization,
        String userId, String jurisdictionId, String caseType, String caseId, String eventId) {
        log.info("Calling startEventForCaseWorker");
        return coreCaseDataApi.startEventForCaseWorker(authorisation, serviceAuthorization,
            userId, jurisdictionId, caseType, caseId, eventId);
    }

    @Retryable(value = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public CaseDetails submitEventForCaseWorker(String authorisation, String serviceAuthorisation,
        String userId, String jurisdictionId, String caseType, String caseId, boolean ignoreWarning,
        CaseDataContent caseDataContent) {
        log.info("Calling submitEventForCaseWorker");
        return coreCaseDataApi.submitEventForCaseWorker(authorisation, serviceAuthorisation, userId,
            jurisdictionId, caseType, caseId, ignoreWarning, caseDataContent);
    }

}
