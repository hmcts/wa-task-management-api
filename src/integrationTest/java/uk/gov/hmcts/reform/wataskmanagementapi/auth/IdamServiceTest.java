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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({"integration"})
public class IdamServiceTest {

    @MockBean
    private IdamWebApi idamWebApi;

    @Autowired
    private IdamService idamService;

    @Test
    public void getUserInfo() {
        when(idamWebApi.userInfo(anyString())).thenReturn(UserInfo.builder()
                                                              .uid("some user id")
                                                              .build());

        idamService.getUserInfo("some user token");
        idamService.getUserInfo("some user token");
        idamService.getUserInfo("some user token");

        verify(idamWebApi).userInfo("some user token");
    }


}
