package uk.gov.hmcts.reform.wataskmanagementapi.services.taskconfiguration;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskconfigurationapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskconfigurationapi.clients.CcdDataServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CcdDataServiceApi;

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
