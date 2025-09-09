package uk.gov.hmcts.reform.wataskmanagementapi.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"integration"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IdamServiceUserIdCacheTest {

    private final String bearerAccessToken1 = "some bearer access token3";
    private final String bearerAccessToken2 = "some bearer access token4";

    @MockitoBean
    private IdamWebApi idamWebApi;

    @Autowired
    private IdamService idamService;

    @Test
    void getUserIdIsCached() {

        when(idamWebApi.userInfo(anyString()))
            .thenReturn(UserInfo.builder()
                .uid("some user id1")
                .build());

        idamService.getUserId(bearerAccessToken1);
        idamService.getUserId(bearerAccessToken1);
        idamService.getUserId(bearerAccessToken2);
        idamService.getUserId(bearerAccessToken2);

        verify(idamWebApi, times(1)).userInfo(bearerAccessToken1);
        verify(idamWebApi, times(1)).userInfo(bearerAccessToken2);

    }
}
