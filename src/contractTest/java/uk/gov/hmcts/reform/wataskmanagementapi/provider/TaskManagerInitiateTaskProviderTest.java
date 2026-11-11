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
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractProviderBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.ExclusiveTaskActionsController;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;

@Provider("wa_task_management_api_initiate_task_by_id")
public class TaskManagerInitiateTaskProviderTest extends SpringBootContractProviderBaseTest {

    private static final String TASK_ID = "704c8b1c-e89b-436a-90f6-953b1dc40157";
    private static final String TASK_NAME = "Process Application";
    private static final String TASK_TYPE = "processApplication";
    private static final String CASE_ID = "1234567890123456";

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void beforeEach(PactVerificationContext context) {
        ExclusiveTaskActionsController controller = new ExclusiveTaskActionsController(
            clientAccessControlService,
            taskManagementService
        );
        ReflectionTestUtils.setField(
            controller,
            "initiationRequestRequiredFields",
            List.of("name", "taskType", "caseId")
        );

        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(controller);
        testTarget.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper));
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State("initiate a task using taskId")
    public void initiateTaskById() {
        TaskResource task = new TaskResource(TASK_ID, TASK_NAME, TASK_TYPE, UNASSIGNED, CASE_ID);

        when(clientAccessControlService.hasExclusiveAccess(anyString())).thenReturn(true);
        when(taskManagementService.initiateTask(eq(TASK_ID), any())).thenReturn(task);
        doNothing().when(taskManagementService).updateTaskIndex(TASK_ID);
    }
}
