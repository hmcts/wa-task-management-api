package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "idam_user_info_cache", key = "#accessToken", sync = true)
    public UserInfo getUserInfo(String accessToken) {
        requireNonNull(accessToken, "access token must not be null");
        return idamWebApi.userInfo(accessToken);
    }

    @Cacheable(value = "idam_user_id_cache", key = "#accessToken", sync = true)
    public String getUserId(String accessToken) {
        UserInfo userInfo = getUserInfo(accessToken);
        requireNonNull(userInfo.getUid(), "User id must not be null");
        return userInfo.getUid();
    }
}
