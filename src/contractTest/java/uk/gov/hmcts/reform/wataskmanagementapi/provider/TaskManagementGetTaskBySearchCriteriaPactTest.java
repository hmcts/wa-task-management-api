package uk.gov.hmcts.reform.wataskmanagementapi.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractProviderBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskSearchController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WarningValues;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("wa_task_management_api_search")
public class TaskManagementGetTaskBySearchCriteriaPactTest extends SpringBootContractProviderBaseTest {

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

        testTarget.setMessageConverters(mappingJackson2HttpMessageConverter);

    }

    @State({"appropriate tasks are returned by criteria"})
    public void getTasksBySearchCriteria() {
        setInitMockForSearchTask();
    }

    @State({"appropriate tasks are returned by criteria for jurisdiction type wa"})
    public void getWaTasksBySearchCriteria() {
        setInitMockForSearchWaTask();
    }

    @State({"appropriate tasks are returned by criteria with work-type"})
    public void getTasksBySearchCriteriaWithWorkType() {
        setInitMockForSearchTask();
    }

    @State({"appropriate tasks are returned by criteria with no warnings"})
    public void getTasksBySearchCriteriaWithNoWarnings() {
        setInitMockForSearchTaskWithNoWarnings();
    }

    @State({"appropriate tasks are returned by criteria with warnings only"})
    public void getTasksBySearchCriteriaWithWarningsOnly() {
        setInitMockForSearchTaskWithWarningsOnly();
    }

    @State({"appropriate tasks are returned by criteria with work-type with warnings only"})
    public void getTasksBySearchCriteriaWithWorkTypeWithWarningsOnly() {
        setInitMockForSearchTaskWithWarningsOnly();
    }

    @State({"appropriate tasks are returned by criteria with role category"})
    public void getTasksBySearchCriteriaWithRoleCategory() {
        setInitMockForSearchTaskWithRoleCategory();
    }

    @State({"appropriate tasks are returned by criteria with context available task"})
    public void getTasksBySearchCriteriaWithAvailableTasksContext() {
        setInitMockForSearchTaskWithWarningsOnly();
    }

    @State({"appropriate tasks are returned by criteria with context all work"})
    public void getTasksBySearchCriteriaWithAllWorkContext() {
        setInitMockForSearchTaskWithWarningsOnly();
    }

    @State({"appropriate tasks are returned by criteria with task type"})
    public void getTasksBySearchCriteriaWithTaskType() {
        setInitMockForSearchTaskWithTaskType();
    }

    public Task createTaskWithNoWarnings() {
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

        return new Task(
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
            false,
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
    }

    public Task createTaskWithWarnings() {
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

        final List<Warning> warnings = List.of(
            new Warning("Code1", "Text1")
        );

        return new Task(
            "fda422de-b381-43ff-94ea-eea5790188a3",
            "Review the appeal",
            "reviewTheAppeal",
            "unassigned",
            "SELF",
            "PUBLIC",
            "Review the appeal",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            null,
            true,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245308495",
            "refusalOfHumanRights",
            "John Doe",
            true,
            new WarningValues(warnings),
            "Some Case Management Category",
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
    }

    public Task createWaTask() {
        final TaskPermissions permissions = new TaskPermissions(
            Set.of(
                PermissionTypes.READ,
                PermissionTypes.EXECUTE,
                PermissionTypes.REFER,
                PermissionTypes.COMPLETE,
                PermissionTypes.ASSIGN,
                PermissionTypes.UNASSIGN
            )
        );

        return new Task(
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
    }

    public Task createTaskForRoleCategorySearch() {
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

        return new Task(
            "4d4b6fgh-c91f-433f-92ac-e456ae34f72a",
            "review appeal skeleton argument",
            "reviewAppealSkeletonArgument",
            "unassigned",
            "SELF",
            "PUBLIC",
            "review appeal skeleton argument",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "10bac6bf-80a7-4c81-b2db-516aba826be6",
            false,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245335311",
            "Protection",
            "Bob Smith",
            false,
            new WarningValues(Collections.emptyList()),
            "Case Management Category",
            "hearing_work",
            "Hearing work",
            permissions,
            RoleCategory.CTSC.name(),
            "aDescription",
            getAdditionalProperties(),
            "nextHearingId",
            ZonedDateTime.now(),
            500,
            5000,
            ZonedDateTime.now()
        );
    }

    public Task createTaskForTaskTypeSearch() {
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

        return new Task(
            "b1a13dca-41a5-424f-b101-c67b439549d0",
            "review appeal skeleton argument",
            "reviewAppealSkeletonArgument",
            "assigned",
            "SELF",
            "PUBLIC",
            "review appeal skeleton argument",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "10bac6bf-80a7-4c81-b2db-516aba826be6",
            true,
            "Case Management Task",
            "WA",
            "1",
            "765324",
            "Taylor House",
            "WaCaseType",
            "1617708245335399",
            "Protection",
            "Bob Smith",
            false,
            new WarningValues(Collections.emptyList()),
            "Some Case Management Category",
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
    }

    private void setInitMockForSearchTask() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(userInfo.getEmail()).thenReturn("test@test.com");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(new GetTasksResponse<>(List.of(createTaskWithNoWarnings(), createTaskWithNoWarnings()), 2L));
    }

    private void setInitMockForSearchWaTask() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(userInfo.getEmail()).thenReturn("test@test.com");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(new GetTasksResponse<>(List.of(createWaTask(), createWaTask()), 2L));
    }

    private void setInitMockForSearchTaskWithWarningsOnly() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(new GetTasksResponse<>(List.of(createTaskWithWarnings()), 1L));
    }


    private void setInitMockForSearchTaskWithNoWarnings() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(new GetTasksResponse<>(List.of(createTaskWithNoWarnings()), 1L));
    }

    private void setInitMockForSearchTaskWithRoleCategory() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(new GetTasksResponse<>(List.of(createTaskForRoleCategorySearch()), 1L));
    }

    private void setInitMockForSearchTaskWithTaskType() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(new GetTasksResponse<>(List.of(createTaskForTaskTypeSearch()), 1L));
    }
}
