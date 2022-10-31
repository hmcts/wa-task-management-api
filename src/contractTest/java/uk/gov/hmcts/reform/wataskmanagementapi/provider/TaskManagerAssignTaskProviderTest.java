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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("wa_task_management_api_assign_task_by_id")
public class TaskManagerAssignTaskProviderTest extends SpringBootContractProviderBaseTest {

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
            launchDarklyFeatureFlagProvider
        ));
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State({"assign a task using taskId"})
    public void assignTaskById() {
        setInitMock();
    }

    private void setInitMock() {
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));

        doNothing().when(taskManagementService).assignTask(any(), any(), any());
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(accessControlService.getRolesGivenUserId(anyString(), anyString())).thenReturn(accessControlResponse);
    }
}
