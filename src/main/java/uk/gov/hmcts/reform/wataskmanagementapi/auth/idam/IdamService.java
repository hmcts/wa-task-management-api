package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;

import static java.util.Objects.requireNonNull;

@CacheConfig(cacheNames={"idamDetails"})
@Component
public class IdamService {

    private final IdamWebApi idamWebApi;

    @Autowired
    public IdamService(IdamWebApi idamWebApi) {
        this.idamWebApi = idamWebApi;
    }

    @Cacheable
    public UserInfo getUserInfo(String accessToken) {
        requireNonNull(accessToken, "access token must not be null");
        return idamWebApi.userInfo(accessToken);
    }

    @Cacheable
    public String getUserId(String accessToken) {
        UserInfo userInfo = getUserInfo(accessToken);
        requireNonNull(userInfo.getUid(), "User id must not be null");
        return userInfo.getUid();
    }
}
