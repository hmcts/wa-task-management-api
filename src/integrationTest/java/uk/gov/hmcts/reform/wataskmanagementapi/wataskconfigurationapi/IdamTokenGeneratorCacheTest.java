package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "IA_IDAM_REDIRECT_URI=http://localhost:3002/oauth2/callback")
@ActiveProfiles("integration")
public class IdamTokenGeneratorCacheTest {

    private final String bearerAccessToken1 = "some bearer access token1";
    private final String bearerAccessToken2 = "some bearer access token2";
    private final String bearerAccessToken3 = "some bearer access token3";
    private final String bearerAccessToken4 = "some bearer access token4";

    @MockBean
    private IdamWebApi idamWebApi;

    @Autowired
    private IdamTokenGenerator systemUserIdamToken;

    @Test
    void getUserInfoIsCached() throws Exception {
        when(idamWebApi.userInfo(anyString()))
            .thenReturn(UserInfo.builder()
                .uid("some user id")
                .build());

        systemUserIdamToken.getUserInfo(bearerAccessToken1);
        systemUserIdamToken.getUserInfo(bearerAccessToken1);
        systemUserIdamToken.getUserInfo(bearerAccessToken1);

        systemUserIdamToken.getUserInfo(bearerAccessToken2);
        systemUserIdamToken.getUserInfo(bearerAccessToken2);
        systemUserIdamToken.getUserInfo(bearerAccessToken2);

        verify(idamWebApi, times(1)).userInfo(bearerAccessToken1);
        verify(idamWebApi, times(1)).userInfo(bearerAccessToken2);
    }

    @Test
    void reloadUserInfoAfterCacheIsExpired() throws Exception {
        when(idamWebApi.userInfo(anyString()))
            .thenReturn(UserInfo.builder()
                .uid("some user id1")
                .build())
            .thenReturn(UserInfo.builder()
                .uid("some user id2")
                .build())
            .thenReturn(UserInfo.builder()
                .uid("some user id3")
                .build())
            .thenReturn(UserInfo.builder()
                .uid("some user id4")
                .build());

        final UserInfo userInfo1 = systemUserIdamToken.getUserInfo(bearerAccessToken3);
        final UserInfo userInfo2 = systemUserIdamToken.getUserInfo(bearerAccessToken4);

        assertEquals("some user id1", userInfo1.getUid());
        assertEquals("some user id2", userInfo2.getUid());

        // cache timeout is set to 5 seconds
        Thread.sleep(5000);

        // after expiry
        final UserInfo userInfo3 = systemUserIdamToken.getUserInfo(bearerAccessToken3);
        final UserInfo userInfo4 = systemUserIdamToken.getUserInfo(bearerAccessToken4);

        assertEquals("some user id3", userInfo3.getUid());
        assertEquals("some user id4", userInfo4.getUid());

        verify(idamWebApi, times(2)).userInfo(bearerAccessToken3);
        verify(idamWebApi, times(2)).userInfo(bearerAccessToken4);
    }

    @Test
    void generate() {
        when(idamWebApi.token(anyMap())).thenReturn(new Token(bearerAccessToken1, "some scope"));

        String someUsername = "some username";
        String someUserPassword = "some user password";
        systemUserIdamToken.getUserBearerToken(someUsername, someUserPassword);
        systemUserIdamToken.getUserBearerToken(someUsername, someUserPassword);
        systemUserIdamToken.getUserBearerToken(someUsername, someUserPassword);
        systemUserIdamToken.getUserBearerToken(someUsername, someUserPassword);

        String someOtherUsername = "some other username";
        String someOtherUserPassword = "some other user password";
        systemUserIdamToken.getUserBearerToken(someOtherUsername, someOtherUserPassword);
        systemUserIdamToken.getUserBearerToken(someOtherUsername, someOtherUserPassword);


        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("redirect_uri", "http://localhost:3002/oauth2/callback");
        map.add("client_id", "wa");
        map.add("client_secret", "something");
        map.add("username", someUsername);
        map.add("password", someUserPassword);
        map.add("scope", "openid profile roles");
        verify(idamWebApi).token(map);

        MultiValueMap<String, String> map2 = new LinkedMultiValueMap<>();
        map2.add("grant_type", "password");
        map2.add("redirect_uri", "http://localhost:3002/oauth2/callback");
        map2.add("client_id", "wa");
        map2.add("client_secret", "something");
        map2.add("username", someOtherUsername);
        map2.add("password", someOtherUserPassword);
        map2.add("scope", "openid profile roles");
        verify(idamWebApi).token(map2);
    }

}
