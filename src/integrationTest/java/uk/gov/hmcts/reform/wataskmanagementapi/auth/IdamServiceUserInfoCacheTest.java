package uk.gov.hmcts.reform.wataskmanagementapi.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;

import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles({"integration"})
public class IdamServiceUserInfoCacheTest {

    @MockBean
    private IdamWebApi idamWebApi;

    @Autowired
    private IdamService idamService;

    @Test
    void getUserInfoIsCached() {
        idamService.getUserInfo("some user token");
        idamService.getUserInfo("some user token");
        idamService.getUserInfo("some user token");

        verify(idamWebApi).userInfo("some user token");
    }

}
