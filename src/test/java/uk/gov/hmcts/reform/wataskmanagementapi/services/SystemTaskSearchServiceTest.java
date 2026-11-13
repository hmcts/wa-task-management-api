package uk.gov.hmcts.reform.wataskmanagementapi.services;

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
import uk.gov.hmcts.reform.wataskmanagementapi.config.TaskSearchConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemTaskSearchServiceTest {

    private static final String TOKEN = "Bearer system-user-token";
    private static final String CASE_ID = "1793986596864362";
    private static final String USERNAME = "probate-search@example.test";

    @Mock
    private IdamWebApi idamWebApi;
    @Mock
    private TaskResourceRepository taskResourceRepository;
    @Mock
    private CFTTaskMapper taskMapper;

    private TaskSearchConfiguration taskSearchConfiguration;
    private SystemTaskSearchService systemTaskSearchService;

    @BeforeEach
    void setUp() {
        taskSearchConfiguration = new TaskSearchConfiguration();
        taskSearchConfiguration.setJurisdictionUsers(Map.of("probate", USERNAME));
        systemTaskSearchService = new SystemTaskSearchService(
            idamWebApi, taskSearchConfiguration, taskResourceRepository, taskMapper);
    }

    @ParameterizedTest
    @MethodSource("unavailableUsers")
    void should_reject_unavailable_idam_user(UserInfo userInfo) {
        when(idamWebApi.userInfo(TOKEN)).thenReturn(userInfo);

        UnAuthorizedException actualException = assertThrows(
            UnAuthorizedException.class,
            () -> systemTaskSearchService.searchTasks(TOKEN, CASE_ID)
        );

        assertThat(actualException).hasMessage("IDAM user not available");

        verifyNoInteractions(taskResourceRepository, taskMapper);
    }

    static Stream<Arguments> unavailableUsers() {
        UserInfo userWithoutUid = UserInfo.builder()
            .email(USERNAME)
            .build();
        UserInfo userWithoutEmail = createUserInfo(null);

        return Stream.of(
            Arguments.of((UserInfo) null),
            Arguments.of(userWithoutUid),
            Arguments.of(userWithoutEmail)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404})
    void should_reject_idam_authentication_failure(int status) {
        FeignException idamException = createIdamException(status);
        when(idamWebApi.userInfo(TOKEN)).thenThrow(idamException);

        UnAuthorizedException actualException = assertThrows(
            UnAuthorizedException.class,
            () -> systemTaskSearchService.searchTasks(TOKEN, CASE_ID)
        );

        assertThat(actualException).hasMessage("IDAM user not available");

        verifyNoInteractions(taskResourceRepository, taskMapper);
    }

    @Test
    void should_reject_user_not_mapped_to_a_jurisdiction() {
        UserInfo userInfo = createUserInfo("normal-user@example.test");
        when(idamWebApi.userInfo(TOKEN)).thenReturn(userInfo);

        GenericForbiddenException actualException = assertThrows(
            GenericForbiddenException.class,
            () -> systemTaskSearchService.searchTasks(TOKEN, CASE_ID)
        );

        assertThat(actualException).hasMessageContaining("User does not belong to any jurisdiction");

        verifyNoInteractions(taskResourceRepository, taskMapper);
    }

    @Test
    void should_deny_access_when_username_configuration_is_empty() {
        taskSearchConfiguration.setJurisdictionUsers(Map.of("probate", ""));
        UserInfo userInfo = createUserInfo(USERNAME);
        when(idamWebApi.userInfo(TOKEN)).thenReturn(userInfo);

        GenericForbiddenException actualException = assertThrows(
            GenericForbiddenException.class,
            () -> systemTaskSearchService.searchTasks(TOKEN, CASE_ID)
        );

        assertThat(actualException).hasMessageContaining("User does not belong to any jurisdiction");

        verifyNoInteractions(taskResourceRepository, taskMapper);
    }

    private static UserInfo createUserInfo(String email) {
        return UserInfo.builder()
            .uid("system-user-id")
            .email(email)
            .build();
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
