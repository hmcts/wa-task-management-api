package uk.gov.hmcts.reform.wataskmanagementapi.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractProviderBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskSearchController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WarningValues;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("wa_task_management_api_search_completable")
public class TaskManagementGetTaskBySearchForCompletablePactTest extends SpringBootContractProviderBaseTest {

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new TaskSearchController(
            accessControlService,
            cftQueryService,
            cftTaskDatabaseService,
            launchDarklyFeatureFlagProvider
        ));

        if (context != null) {
            context.setTarget(testTarget);
        }

        testTarget.setMessageConverters((
            new MappingJackson2HttpMessageConverter(
                objectMapper
            )));

    }

    @State({"appropriate tasks are returned by search for completable"})
    public void getTasksBySearchForCompletableCriteria() {
        setInitMockForSearchByCompletableTask();
    }

    @State({"appropriate wa tasks are returned by search for completable"})
    public void getWaTasksBySearchForCompletableCriteria() {
        setInitMockForSearchByCompletableWaTask();
    }

    @State({"appropriate tasks are returned by search for completable with warnings"})
    public void getTasksBySearchForCompletableCriteriaWithWarnings() {
        setInitMockForSearchByCompletableTaskWithWarnings();
    }

    public List<Task> createTasks() {
        final TaskPermissions permissions = new TaskPermissions(
            Set.of(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.MANAGE,
                PermissionTypes.REFER
            )
        );

        Task task = new Task(
            "4d4b6fgh-c91f-433f-92ac-e456ae34f72a",
            "Review the appeal",
            "reviewTheAppeal",
            "assigned",
            "SELF",
            "PUBLIC",
            "Review the appeal",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "10bac6bf-80a7-4c81-b2db-516aba826be6",
            true,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245335311",
            "refusalOfHumanRights",
            "Bob Smith",
            false,
            new WarningValues(Collections.emptyList()),
            "Case Management Category",
            "hearing_work",
            "Hearing work",
            permissions,
            RoleCategory.LEGAL_OPERATIONS.name(),
            "a description",
            getAdditionalProperties(),
            "nextHearingId",
            ZonedDateTime.now(),
            500,
            5000,
            ZonedDateTime.now()
        );
        return singletonList(task);
    }

    public List<Task> createWaTasks() {
        final TaskPermissions permissions = new TaskPermissions(
            Set.of(
                PermissionTypes.READ,
                PermissionTypes.EXECUTE,
                PermissionTypes.REFER,
                PermissionTypes.COMPLETE,
                PermissionTypes.ASSIGN,
                PermissionTypes.UNCLAIM
            )
        );

        Task task = new Task(
            "4d4b6fgh-c91f-433f-92ac-e456ae34f72a",
            "Process Application",
            "processApplication",
            "unassigned",
            "SELF",
            "PUBLIC",
            "Process Application",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            null,
            false,
            "Case Management Task",
            "WA",
            "1",
            "765324",
            "Taylor House",
            "WaCaseType",
            "1617708245335311",
            "Protection",
            "Bob Smith",
            false,
            new WarningValues(Collections.emptyList()),
            "Protection",
            "hearing_work",
            "Hearing work",
            permissions,
            RoleCategory.LEGAL_OPERATIONS.name(),
            "aDescription",
            getAdditionalProperties(),
            "nextHearingId",
            ZonedDateTime.now(),
            500,
            5000,
            ZonedDateTime.now()
        );

        return singletonList(task);
    }

    public List<Task> createTasksWithWarnings() {
        final List<Warning> warnings = List.of(
            new Warning("Code1", "Text1")
        );

        final TaskPermissions permissions = new TaskPermissions(
            Set.of(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.MANAGE,
                PermissionTypes.REFER
            )
        );

        Task taskWithWarnings = new Task(
            "4d4b6fgh-c91f-433f-92ac-e456ae34f72a",
            "Review the appeal",
            "reviewTheAppeal",
            "assigned",
            "SELF",
            "PUBLIC",
            "Review the appeal",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "10bac6bf-80a7-4c81-b2db-516aba826be6",
            true,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245335311",
            "refusalOfHumanRights",
            "Bob Smith",
            true,
            new WarningValues(warnings),
            "Case Management Category",
            "hearing_work",
            "Hearing work",
            permissions,
            RoleCategory.LEGAL_OPERATIONS.name(),
            "a description",
            getAdditionalProperties(),
            "nextHearingId",
            ZonedDateTime.now(),
            500,
            5000,
            ZonedDateTime.now()
        );

        return singletonList(taskWithWarnings);
    }

    private void setInitMockForSearchByCompletableTask() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(userInfo.getEmail()).thenReturn("test@test.com");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);
        when(cftQueryService.searchForCompletableTasks(any(), any(), any(), anyBoolean()))
            .thenReturn(new GetTasksCompletableResponse<>(false, createTasks()));

    }

    private void setInitMockForSearchByCompletableWaTask() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(userInfo.getEmail()).thenReturn("test@test.com");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);
        when(cftQueryService.searchForCompletableTasks(any(), any(), any(), anyBoolean()))
            .thenReturn(new GetTasksCompletableResponse<>(false, createWaTasks()));
    }

    private void setInitMockForSearchByCompletableTaskWithWarnings() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(userInfo.getEmail()).thenReturn("test@test.com");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);

        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);
        when(cftQueryService.searchForCompletableTasks(any(), any(), any(), anyBoolean()))
            .thenReturn(new GetTasksCompletableResponse<>(false, createTasksWithWarnings()));
    }

}
