package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@IntegrationTest(properties = {
    "search.jurisdictionUsers.probate=probate-search@example.test",
    "search.jurisdictionUsers.ia=ia-search@example.test"
})
@Transactional
@Sql("/scripts/system_task_search.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SystemTaskSearchServiceTest {

    private static final String TOKEN = "Bearer system-search-service-test";
    private static final String CASE_ID = "1793986596864362";

    @Autowired
    private SystemTaskSearchService systemTaskSearchService;
    @Autowired
    private IdamWebApi idamWebApi;
    @MockitoSpyBean
    private TaskResourceRepository taskResourceRepository;

    @BeforeEach
    void setUp() {
        reset(idamWebApi);
        clearInvocations(taskResourceRepository);
    }

    @Test
    void should_map_persisted_tasks_only_for_the_users_case_and_jurisdiction() {
        when(idamWebApi.userInfo(TOKEN)).thenReturn(createUserInfo("probate-search@example.test"));

        List<Task> tasks = systemTaskSearchService.searchTasks(TOKEN, CASE_ID);

        assertThat(tasks).extracting(Task::getId, Task::getType, Task::getTaskState,
                                    Task::getCaseId, Task::getJurisdiction)
            .containsExactlyInAnyOrder(
                tuple("70000000-0000-0000-0000-000000000001", "ResolveStoppedCase", "unassigned",
                      CASE_ID, "PROBATE"),
                tuple("70000000-0000-0000-0000-000000000002", "ExamineDigitalCaseProbate", "cancelled",
                      CASE_ID, "PROBATE")
        );
        assertThat(tasks).allSatisfy(task -> assertThat(task.getPermissions().getValues()).isEmpty());
    }

    @Test
    void should_use_the_other_users_configured_jurisdiction_for_the_same_case() {
        when(idamWebApi.userInfo(TOKEN)).thenReturn(createUserInfo("ia-search@example.test"));

        List<Task> tasks = systemTaskSearchService.searchTasks(TOKEN, CASE_ID);

        assertThat(tasks)
            .extracting(Task::getId, Task::getJurisdiction)
            .containsExactly(tuple("70000000-0000-0000-0000-000000000003", "IA"));
    }

    @Test
    void should_revalidate_idam_on_a_subsequent_request() {
        Request request = Request.create(Request.HttpMethod.GET, "http://idam/o/userinfo", Map.of(),
                                         (byte[]) null, null, null);
        FeignException rejection = FeignException.errorStatus("IdamWebApi#userInfo", Response.builder()
            .status(401).reason("Unauthorized").request(request).build());
        when(idamWebApi.userInfo(TOKEN))
            .thenReturn(createUserInfo("probate-search@example.test"))
            .thenThrow(rejection);
        List<Task> initialTasks = systemTaskSearchService.searchTasks(TOKEN, CASE_ID);

        assertThat(initialTasks).hasSize(2);
        clearInvocations(taskResourceRepository);

        assertThatThrownBy(() -> systemTaskSearchService.searchTasks(TOKEN, CASE_ID))
            .isInstanceOf(UnAuthorizedException.class)
            .hasMessage("IDAM user not available");

        verify(taskResourceRepository, never()).findByCaseIdAndJurisdictionIgnoreCase(anyString(), anyString());
    }

    private static UserInfo createUserInfo(String username) {
        return UserInfo.builder().uid("system-search-service-user").email(username).build();
    }
}
