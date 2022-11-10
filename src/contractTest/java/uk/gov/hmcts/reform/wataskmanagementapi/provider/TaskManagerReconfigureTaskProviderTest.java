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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskReconfigurationController;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Provider("wa_task_management_api_reconfigure_task_by_case_id")
public class TaskManagerReconfigureTaskProviderTest extends SpringBootContractProviderBaseTest {

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
        testTarget.setControllers(new TaskReconfigurationController(
            taskManagementService,
            clientAccessControlService
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

    @State({"reconfigure a task using caseId"})
    public void reconfigureTasksByCaseId() {
        setInitMock();
    }

    @State({"reconfigure a task"})
    public void reconfigureTasks() {
        setInitMock();
    }

    private void setInitMock() {
        when(clientAccessControlService.hasExclusiveAccess(anyString())).thenReturn(true);
        when(taskManagementService.performOperation(any())).thenReturn(List.of());
    }
}
