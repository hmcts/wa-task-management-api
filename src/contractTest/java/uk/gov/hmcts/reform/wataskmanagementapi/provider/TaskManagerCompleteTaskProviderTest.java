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
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("wa_task_management_api_complete_task_by_id")
public class TaskManagerCompleteTaskProviderTest extends SpringBootContractProviderBaseTest {

    @State({"complete a task using taskId"})
    public void completeTaskById() {
        setInitMockWithoutPrivilegedAccess();
    }

    @State({"complete a task using taskId and with completion process"})
    public void completeTaskByIdWithCompletionProcess() {
        setInitMockWithoutPrivilegedAccessWithCompletionProcess();
    }

    @State({"complete a task using taskId and assign and complete completion options"})
    public void completeTaskByIdWithCompletionOptions() {
        setInitMockWithPrivilegedAccess();
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

        testTarget.setMessageConverters((
            new MappingJackson2HttpMessageConverter(
                objectMapper
            )));

    }

    private void setInitMockWithoutPrivilegedAccess() {
        doNothing().when(taskManagementService).completeTask(any(), any(), any());
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock((UserInfo.class));
        when(userInfo.getUid()).thenReturn("someUserId");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(clientAccessControlService.hasPrivilegedAccess(any(), any())).thenReturn(false);
    }

    private void setInitMockWithoutPrivilegedAccessWithCompletionProcess() {
        doNothing().when(taskManagementService).completeTask(any(), any(), any());
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock((UserInfo.class));
        when(userInfo.getUid()).thenReturn("someUserId");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(clientAccessControlService.hasPrivilegedAccess(any(), any())).thenReturn(false);
        when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_COMPLETION_PROCESS_UPDATE),
            anyString(), anyString())).thenReturn(true);
        when(completionProcessValidator.validate(anyString(), anyString(), anyBoolean())).thenAnswer(invocation -> {
            if (Math.random() < 0.4) {
                return Optional.of("EXUI_USER_COMPLETION");
            } else if (Math.random() > 0.6) {
                return Optional.of("EXUI_CASE-EVENT_COMPLETION");
            } else {
                return Optional.empty();
            }
        });

    }

    private void setInitMockWithPrivilegedAccess() {
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock((UserInfo.class));
        when(userInfo.getUid()).thenReturn("someUserId");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        doNothing().when(taskManagementService).completeTaskWithPrivilegeAndCompletionOptions(
            any(), any(), any(), any());
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(clientAccessControlService.hasPrivilegedAccess(any(), any())).thenReturn(true);


    }
}
