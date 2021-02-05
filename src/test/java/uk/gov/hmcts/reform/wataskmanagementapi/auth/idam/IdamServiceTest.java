package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamServiceApi;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdamServiceTest {

    @Mock
    private IdamServiceApi idamServiceApi;

    private IdamService idamService;

    @BeforeEach
    public void setUp() {
        idamService = new IdamService(idamServiceApi);
    }

    @Test
    void should_return_user_info() {

        String accessToken = "someAccessToken";

        UserInfo mockedUserInfo = new UserInfo(
            "email@example.com",
            "someUserId",
            asList("role1", "role2"),
            "someName",
            "someGivenName",
            "someFamilyName"
        );

        when(idamServiceApi.userInfo(accessToken)).thenReturn(mockedUserInfo);

        UserInfo userInfoResponse = idamService.getUserInfo(accessToken);

        verify(idamServiceApi, times(1)).userInfo(accessToken);
        verifyNoMoreInteractions(idamServiceApi);

        assertEquals(userInfoResponse, mockedUserInfo);
    }

    @Test
    void should_throw_an_exception_if_id_is_not_set() {

        String accessToken = "someAccessToken";

        UserInfo mockedUserInfo = new UserInfo(
            "email@example.com",
            null,
            asList("role1", "role2"),
            "someName",
            "someGivenName",
            "someFamilyName"
        );

        when(idamServiceApi.userInfo(accessToken)).thenReturn(mockedUserInfo);

        assertThatThrownBy(() -> idamService.getUserId(accessToken))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("User id must not be null")
            .hasNoCause();

        verify(idamServiceApi, times(1)).userInfo(accessToken);
        verifyNoMoreInteractions(idamServiceApi);

    }

    @Test
    void should_return_user_id_only() {

        String accessToken = "someAccessToken";

        UserInfo mockedUserInfo = new UserInfo(
            "email@example.com",
            "someUserId",
            asList("role1", "role2"),
            "someName",
            "someGivenName",
            "someFamilyName"
        );

        when(idamServiceApi.userInfo(accessToken)).thenReturn(mockedUserInfo);

        String userIdResponse = idamService.getUserId(accessToken);

        verify(idamServiceApi, times(1)).userInfo(accessToken);
        verifyNoMoreInteractions(idamServiceApi);

        assertEquals(userIdResponse, mockedUserInfo.getUid());
    }
}
