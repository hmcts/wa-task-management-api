package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.entities.UserIdamTokenGeneratorInfo;

@CacheConfig(cacheNames = {"idamTokenGenerator"})
public class IdamTokenGenerator {

    private final UserIdamTokenGeneratorInfo userIdamTokenGeneratorInfo;
    private final IdamWebApi idamWebApi;

    public IdamTokenGenerator(UserIdamTokenGeneratorInfo userIdamTokenGeneratorInfo,
                              IdamWebApi idamWebApi) {
        this.userIdamTokenGeneratorInfo = userIdamTokenGeneratorInfo;
        this.idamWebApi = idamWebApi;
    }

    public String generate() {
        return getUserBearerToken(
            userIdamTokenGeneratorInfo.getUserName(),
            userIdamTokenGeneratorInfo.getUserPassword()
        );
    }

    @Cacheable(value = "bearer_token_cache", key = "#username")
    public String getUserBearerToken(String username, String password) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("redirect_uri", userIdamTokenGeneratorInfo.getIdamRedirectUrl());
        map.add("client_id", userIdamTokenGeneratorInfo.getIdamClientId());
        map.add("client_secret", userIdamTokenGeneratorInfo.getIdamClientSecret());
        map.add("username", username);
        map.add("password", password);
        map.add("scope", userIdamTokenGeneratorInfo.getIdamScope());
        Token tokenResponse = idamWebApi.token(map);

        return "Bearer " + tokenResponse.getAccessToken();
    }

    @Cacheable(value = "user_info_cache", key = "#bearerAccessToken")
    public UserInfo getUserInfo(String bearerAccessToken) {
        return idamWebApi.userInfo(bearerAccessToken);
    }
}
