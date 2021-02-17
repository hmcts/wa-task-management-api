package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;

import static java.util.Objects.requireNonNull;

@Component
public class IdamService {

    private final IdamWebApi idamWebApi;

    @Autowired
    public IdamService(IdamWebApi idamWebApi) {
        this.idamWebApi = idamWebApi;
    }

    public UserInfo getUserInfo(String accessToken) {
        requireNonNull(accessToken, "access token must not be null");
        return idamWebApi.userInfo(accessToken);
    }

    public String getUserId(String authToken) {
        UserInfo userInfo = getUserInfo(authToken);
        requireNonNull(userInfo.getUid(), "User id must not be null");
        return userInfo.getUid();
    }
}
