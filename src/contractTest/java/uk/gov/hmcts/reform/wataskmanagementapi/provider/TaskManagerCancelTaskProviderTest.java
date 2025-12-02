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
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("wa_task_management_api_cancel_task_by_id")
public class TaskManagerCancelTaskProviderTest extends SpringBootContractProviderBaseTest {

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
            cancellationProcessValidator
        ));
        if (context != null) {
            context.setTarget(testTarget);
        }

    }

    @State({"cancel a task using taskId"})
    public void cancelTaskById() {
        setInitMock();
    }

    @State({"cancel a task using taskId and with cancellation process"})
    public void cancelTaskByIdWithCancellationProcess() {
        setInitMockWithoutPrivilegedAccessWithCancellationProcess();
    }

    private void setInitMock() {
        doNothing().when(taskManagementService).cancelTask(any(), any(), any());
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock((UserInfo.class));
        when(userInfo.getUid()).thenReturn("someUserId");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
    }

    private void setInitMockWithoutPrivilegedAccessWithCancellationProcess() {
        doNothing().when(taskManagementService).cancelTask(any(), any(), any());
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock((UserInfo.class));
        when(userInfo.getUid()).thenReturn("someUserId");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(clientAccessControlService.hasPrivilegedAccess(any(), any())).thenReturn(false);
        when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_CANCELLATION_PROCESS_FEATURE),
                                                             anyString(), anyString())).thenReturn(true);
        when(cancellationProcessValidator.validate(anyString(), anyString(), any())).thenAnswer(invocation -> {
            if (Math.random() < 0.5) {
                return Optional.of("EXUI_USER_CANCELLATION!");
            } else {
                return Optional.empty();
            }
        });

    }
}
