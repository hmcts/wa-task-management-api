package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;

@Service
public class CcdRetryableClient {
    private final CoreCaseDataApi coreCaseDataApi;

    public CcdRetryableClient(CoreCaseDataApi coreCaseDataApi) {
        this.coreCaseDataApi = coreCaseDataApi;
    }

    @Retryable(value = Exception.class, maxAttempts = 2, backoff = @Backoff(delay = 100))
    public CaseDetails submitForCaseworker(String authorisation, String serviceAuthorisation, String userId,
        String jurisdictionId, String caseType, boolean ignoreWarning, CaseDataContent caseDataContent) {
        return coreCaseDataApi.submitForCaseworker(authorisation, serviceAuthorisation, userId, jurisdictionId,
            caseType, ignoreWarning, caseDataContent);
    }

    public StartEventResponse startForCaseworker(String authorisation, String serviceAuthorization,
        String userId, String jurisdictionId, String caseType, String eventId) {
        return coreCaseDataApi.startForCaseworker(authorisation, serviceAuthorization,
            userId, jurisdictionId, caseType, eventId);
    }

    public StartEventResponse startEventForCaseWorker(String authorisation, String serviceAuthorization,
        String userId, String jurisdictionId, String caseType, String caseId, String eventId) {
        return coreCaseDataApi.startEventForCaseWorker(authorisation, serviceAuthorization,
            userId, jurisdictionId, caseType, caseId, eventId);
    }

    public CaseDetails submitEventForCaseWorker(String authorisation, String serviceAuthorisation,
        String userId, String jurisdictionId, String caseType, String caseId, boolean ignoreWarning,
        CaseDataContent caseDataContent) {
        return coreCaseDataApi.submitEventForCaseWorker(authorisation, serviceAuthorisation, userId,
            jurisdictionId, caseType, caseId, ignoreWarning, caseDataContent);
    }

}
