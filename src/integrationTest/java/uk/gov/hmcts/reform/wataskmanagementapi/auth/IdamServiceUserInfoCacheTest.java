package uk.gov.hmcts.reform.wataskmanagementapi.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"integration"})
public class IdamServiceUserInfoCacheTest {

    private final String bearerAccessToken1 = "some bearer access token1";
    private final String bearerAccessToken2 = "some bearer access token2";
    private final String bearerAccessToken3 = "some bearer access token3";
    private final String bearerAccessToken4 = "some bearer access token4";

    @MockBean
    private IdamWebApi idamWebApi;

    @Autowired
    private IdamService idamService;

    @Test
    void getUserInfoIsCached() {

        when(idamWebApi.userInfo(anyString()))
            .thenReturn(UserInfo.builder()
                .uid("some user id1")
                .build());

        idamService.getUserInfo(bearerAccessToken1);
        idamService.getUserInfo(bearerAccessToken1);
        idamService.getUserInfo(bearerAccessToken2);
        idamService.getUserInfo(bearerAccessToken2);


        verify(idamWebApi, times(1)).userInfo(bearerAccessToken1);
        verify(idamWebApi, times(1)).userInfo(bearerAccessToken2);
    }

    @Test
    void getUserIdIsCached() {

        when(idamWebApi.userInfo(anyString()))
            .thenReturn(UserInfo.builder()
                .uid("some user id1")
                .build());

        idamService.getUserId(bearerAccessToken3);
        idamService.getUserId(bearerAccessToken3);
        idamService.getUserId(bearerAccessToken4);
        idamService.getUserId(bearerAccessToken4);


        verify(idamWebApi, times(1)).userInfo(bearerAccessToken3);
        verify(idamWebApi, times(1)).userInfo(bearerAccessToken4);
    }

}
