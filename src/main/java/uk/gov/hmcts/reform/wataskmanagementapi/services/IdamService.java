package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;

import static java.util.Objects.requireNonNull;

@Component
public class IdamService {

    private final IdamServiceApi idamServiceApi;

    @Autowired
    public IdamService(IdamServiceApi idamServiceApi) {
        this.idamServiceApi = idamServiceApi;
    }

    public UserInfo getUserInfo(String accessToken) {
        requireNonNull(accessToken, "access token must not be null");
        return idamServiceApi.userInfo(accessToken);
    }

    public String getUserId(String authToken) {
        UserInfo userInfo = getUserInfo(authToken);
        requireNonNull(userInfo.getUid(), "User id must not be null");
        return userInfo.getUid();
    }
}
