package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdamServiceTest {

    private static final String TOKEN = "Bearer system-user-token";
    private static final String USERNAME = "probate-search@example.test";

    @Mock
    private IdamWebApi idamWebApi;

    private IdamService idamService;

    @BeforeEach
    void setUp() {
        idamService = new IdamService(idamWebApi);
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

        when(idamWebApi.userInfo(accessToken)).thenReturn(mockedUserInfo);

        UserInfo userInfoResponse = idamService.getUserInfo(accessToken);

        verify(idamWebApi, times(1)).userInfo(accessToken);
        verifyNoMoreInteractions(idamWebApi);

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

        when(idamWebApi.userInfo(accessToken)).thenReturn(mockedUserInfo);

        assertThatThrownBy(() -> idamService.getUserId(accessToken))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("User id must not be null")
            .hasNoCause();

        verify(idamWebApi, times(1)).userInfo(accessToken);
        verifyNoMoreInteractions(idamWebApi);

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

        when(idamWebApi.userInfo(accessToken)).thenReturn(mockedUserInfo);

        String userIdResponse = idamService.getUserId(accessToken);

        verify(idamWebApi, times(1)).userInfo(accessToken);
        verifyNoMoreInteractions(idamWebApi);

        assertEquals(userIdResponse, mockedUserInfo.getUid());
    }

    @ParameterizedTest
    @MethodSource("unavailableUsers")
    void should_reject_unavailable_idam_user_for_authorization(UserInfo userInfo) {
        when(idamWebApi.userInfo(TOKEN)).thenReturn(userInfo);

        UnAuthorizedException actualException = assertThrows(
            UnAuthorizedException.class,
            () -> idamService.getUserInfoForAuthorization(TOKEN)
        );

        assertThat(actualException).hasMessage("IDAM user not available");
    }

    static Stream<Arguments> unavailableUsers() {
        UserInfo userWithoutUid = UserInfo.builder()
            .email(USERNAME)
            .build();
        UserInfo userWithoutEmail = UserInfo.builder()
            .uid("system-user-id")
            .build();

        return Stream.of(
            Arguments.of((UserInfo) null),
            Arguments.of(userWithoutUid),
            Arguments.of(userWithoutEmail)
        );
    }

    @Test
    void should_reject_idam_client_error_for_authorization() {
        FeignException idamException = createIdamException(400);
        when(idamWebApi.userInfo(TOKEN)).thenThrow(idamException);

        UnAuthorizedException actualException = assertThrows(
            UnAuthorizedException.class,
            () -> idamService.getUserInfoForAuthorization(TOKEN)
        );

        assertThat(actualException).hasMessage("IDAM user not available");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 429})
    void should_propagate_idam_dependency_failure_for_authorization(int status) {
        FeignException idamException = createIdamException(status);
        when(idamWebApi.userInfo(TOKEN)).thenThrow(idamException);

        FeignException actualException = assertThrows(
            FeignException.class,
            () -> idamService.getUserInfoForAuthorization(TOKEN)
        );

        assertThat(actualException).isSameAs(idamException);
    }

    private static FeignException createIdamException(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://idam/o/userinfo", Map.of(),
                                         (byte[]) null, null, null);
        Response response = Response.builder()
            .status(status)
            .reason("IDAM failure")
            .request(request)
            .build();

        return FeignException.errorStatus("IdamWebApi#userInfo", response);
    }
}
