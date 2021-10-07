package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;

@Component
public class LaunchDarklyClient {

    @Autowired private IdamWebApi idamWebApi;
    private final LDClientInterface ldClient;

    @Autowired
    public LaunchDarklyClient(LDClientInterface ldClient) {
        this.ldClient = ldClient;
    }

    public boolean getKey(String key, String accessToken) {

        UserInfo userInfo = getUserInfo(accessToken);

        LDUser ldUser =  new LDUser.Builder(userInfo.getUid())
            .firstName(userInfo.getName())
            .lastName(userInfo.getFamilyName())
            .email(userInfo.getEmail())
            .build();

        return ldClient.boolVariation(key, ldUser, false);
    }

    private UserInfo getUserInfo(String accessToken) {

        UserInfo userInfo = idamWebApi.userInfo(accessToken);

        return new UserInfo(
            accessToken,
            userInfo.getUid(),
            userInfo.getRoles(),
            userInfo.getEmail(),
            userInfo.getGivenName(),
            userInfo.getFamilyName()
        );
    }
}
