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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskRolePermissions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("wa_task_management_api_task_role_permissions_by_task_id")
public class TaskManagementGetTaskRolePermissionsTest extends SpringBootContractProviderBaseTest {

    @State({"get task role information using taskId"})
    public void getTaskRolePermissionsByTaskId() {
        setInitMockTask();
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
        testTarget.setControllers(new TaskActionsController(
            taskManagementService,
            accessControlService,
            systemDateProvider,
            clientAccessControlService,
            taskDeletionService,
            completionProcessValidator,
            launchDarklyFeatureFlagProvider
        ));

        if (context != null) {
            context.setTarget(testTarget);
        }

        testTarget.setMessageConverters(
            (
                new MappingJackson2HttpMessageConverter(objectMapper)
            )
        );

    }

    private void setInitMockTask() {
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock((UserInfo.class));
        when(userInfo.getUid()).thenReturn("someUserId");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(taskManagementService.getTaskRolePermissions(any(), any())).thenReturn(createTaskRolePermissions());
    }

    private List<TaskRolePermissions> createTaskRolePermissions() {
        final String roleCategory = "LEGAL_OPERATIONS";
        TaskRolePermissions taskRolePermissions = new TaskRolePermissions(
            roleCategory,
            "tribunal-caseworker",
            List.of(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.COMPLETE,
                PermissionTypes.COMPLETE_OWN,
                PermissionTypes.CANCEL_OWN,
                PermissionTypes.CLAIM,
                PermissionTypes.UNCLAIM,
                PermissionTypes.ASSIGN,
                PermissionTypes.UNASSIGN,
                PermissionTypes.UNCLAIM_ASSIGN,
                PermissionTypes.UNASSIGN_CLAIM,
                PermissionTypes.UNASSIGN_ASSIGN
                ),
            List.of("IAC", "SCSS")
        );

        return List.of(taskRolePermissions);
    }


}
