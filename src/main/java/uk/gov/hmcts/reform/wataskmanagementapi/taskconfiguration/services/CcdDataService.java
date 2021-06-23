package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CcdDataServiceApi;

@Component
public class CcdDataService {
    private final CcdDataServiceApi ccdDataServiceApi;
    private final AuthTokenGenerator serviceAuthTokenGenerator;
    private final IdamTokenGenerator systemUserIdamToken;

    public CcdDataService(
        CcdDataServiceApi ccdDataServiceApi,
        AuthTokenGenerator serviceAuthTokenGenerator,
        IdamTokenGenerator systemUserIdamToken
    ) {
        this.ccdDataServiceApi = ccdDataServiceApi;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
        this.systemUserIdamToken = systemUserIdamToken;
    }

    public String getCaseData(String caseId) {
        return ccdDataServiceApi.getCase(
            systemUserIdamToken.generate(),
            serviceAuthTokenGenerator.generate(),
            caseId
        );
    }
}
