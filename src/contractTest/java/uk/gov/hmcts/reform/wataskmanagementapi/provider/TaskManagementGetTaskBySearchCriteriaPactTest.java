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
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskSearchController;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("wa_task_management_api_search")
public class TaskManagementGetTaskBySearchCriteriaPactTest extends SpringBootContractProviderBaseTest {

    @State({"appropriate tasks are returned by criteria"})
    public void getTasksBySearchCriteria() {
        setInitMockForSearchTask();
    }

    @State({"appropriate tasks are returned by criteria for jurisdiction type wa"})
    public void getWaTasksBySearchCriteria() {
        setInitMockForSearchWaTask();
    }

    @State({"appropriate tasks are returned by criteria with available tasks only"})
    public void getTasksBySearchCriteriaWithAvailableTasksOnly() {
        setInitMockForSearchTask();
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
                PermissionTypes.REFER
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
            taskManagementService,
            accessControlService,
            cftQueryService,
            launchDarklyFeatureFlagProvider
        ));

        if (context != null) {
            context.setTarget(testTarget);
        }

        testTarget.setMessageConverters(mappingJackson2HttpMessageConverter);

    }

    private void setInitMockForSearchTask() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(userInfo.getEmail()).thenReturn("test@test.com");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                FeatureFlag.RELEASE_2_TASK_QUERY, accessControlResponse.get().getUserInfo().getUid(),
                accessControlResponse.get().getUserInfo().getEmail()
            )
        ).thenReturn(false);

        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any()))
            .thenReturn(asList(createTaskWithNoWarnings(), createTaskWithNoWarnings()));
    }

    private void setInitMockForSearchWaTask() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(userInfo.getEmail()).thenReturn("test@test.com");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                FeatureFlag.RELEASE_2_TASK_QUERY, accessControlResponse.get().getUserInfo().getUid(),
                accessControlResponse.get().getUserInfo().getEmail()
            )
        ).thenReturn(false);

        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any()))
            .thenReturn(asList(createWaTask(), createWaTask()));
    }

    private void setInitMockForSearchTaskWithWarningsOnly() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                FeatureFlag.RELEASE_2_TASK_QUERY, accessControlResponse.get().getUserInfo().getUid(),
                accessControlResponse.get().getUserInfo().getEmail()
            )
        ).thenReturn(false);
        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any()))
            .thenReturn(singletonList(createTaskWithWarnings()));
    }


    private void setInitMockForSearchTaskWithNoWarnings() {
        Optional<AccessControlResponse> accessControlResponse = Optional.of(mock((AccessControlResponse.class)));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(accessControlResponse.get().getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getAccessControlResponse(anyString()))
            .thenReturn(accessControlResponse);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                FeatureFlag.RELEASE_2_TASK_QUERY, accessControlResponse.get().getUserInfo().getUid(),
                accessControlResponse.get().getUserInfo().getEmail()
            )
        ).thenReturn(false);

        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any()))
            .thenReturn(singletonList(createTaskWithNoWarnings()));
    }

}
