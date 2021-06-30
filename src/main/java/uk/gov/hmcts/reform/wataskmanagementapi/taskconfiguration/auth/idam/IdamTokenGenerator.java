package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.entities.UserIdamTokenGeneratorInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.IdamServiceApi;

public class IdamTokenGenerator {

    private final UserIdamTokenGeneratorInfo userIdamTokenGeneratorInfo;
    private final IdamServiceApi idamServiceApi;

    public IdamTokenGenerator(UserIdamTokenGeneratorInfo userIdamTokenGeneratorInfo,
                              IdamServiceApi idamServiceApi) {
        this.userIdamTokenGeneratorInfo = userIdamTokenGeneratorInfo;
        this.idamServiceApi = idamServiceApi;
    }

    public String generate() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("redirect_uri", userIdamTokenGeneratorInfo.getIdamRedirectUrl());
        map.add("client_id", userIdamTokenGeneratorInfo.getIdamClientId());
        map.add("client_secret", userIdamTokenGeneratorInfo.getIdamClientSecret());
        map.add("username", userIdamTokenGeneratorInfo.getUserName());
        map.add("password", userIdamTokenGeneratorInfo.getUserPassword());
        map.add("scope", userIdamTokenGeneratorInfo.getIdamScope());
        Token tokenResponse = idamServiceApi.token(map);

        return "Bearer " + tokenResponse.getAccessToken();
    }

    public UserInfo getUserInfo(String bearerAccessToken) {
        return idamServiceApi.userInfo(bearerAccessToken);
    }
}
