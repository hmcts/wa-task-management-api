package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CaffeineConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.entities.UserIdamTokenGeneratorInfo;

@CacheConfig(cacheNames = {CaffeineConfiguration.CACHE_NAME})
@Component
public class IdamTokenGenerator {

    private final UserIdamTokenGeneratorInfo systemUserIdamInfo;
    private final IdamWebApi idamWebApi;

    @Autowired
    public IdamTokenGenerator(UserIdamTokenGeneratorInfo systemUserIdamInfo,
                              IdamWebApi idamWebApi) {
        this.systemUserIdamInfo = systemUserIdamInfo;
        this.idamWebApi = idamWebApi;
    }

    public String generate() {
        return getUserBearerToken(
            systemUserIdamInfo.getUserName(),
            systemUserIdamInfo.getUserPassword()
        );
    }

    @Cacheable(value = "bearer_token_cache", key = "#username")
    public String getUserBearerToken(String username, String password) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("redirect_uri", systemUserIdamInfo.getIdamRedirectUrl());
        map.add("client_id", systemUserIdamInfo.getIdamClientId());
        map.add("client_secret", systemUserIdamInfo.getIdamClientSecret());
        map.add("username", username);
        map.add("password", password);
        map.add("scope", systemUserIdamInfo.getIdamScope());
        Token tokenResponse = idamWebApi.token(map);

        return "Bearer " + tokenResponse.getAccessToken();
    }

    @Cacheable(value = "user_info_cache", key = "#bearerAccessToken")
    public UserInfo getUserInfo(String bearerAccessToken) {
        return idamWebApi.userInfo(bearerAccessToken);
    }
}
