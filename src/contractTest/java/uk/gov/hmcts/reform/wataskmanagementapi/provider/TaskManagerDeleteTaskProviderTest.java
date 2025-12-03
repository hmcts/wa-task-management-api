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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//TODO: remove entire class if there isn't a use for it with the new update action behavior

@Provider("wa_task_management_api_delete_task_by_id")
public class TaskManagerDeleteTaskProviderTest extends SpringBootContractProviderBaseTest {

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
            completionProcessValidator,
            cancellationProcessValidator
        ));
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State({"delete a task using case reference id"})
    public void deleteTaskById() {
        setInitMock();
    }

    private void setInitMock() {
        UserInfo userInfo = mock((UserInfo.class));
        when(userInfo.getUid()).thenReturn("someUserId");
        when(clientAccessControlService.hasPrivilegedAccess(any())).thenReturn(true);
    }
}
