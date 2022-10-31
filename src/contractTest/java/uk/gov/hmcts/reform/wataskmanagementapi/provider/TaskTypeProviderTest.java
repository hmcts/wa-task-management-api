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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskTypesController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype.TaskType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype.TaskTypeResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("wa_task_management_api_get_task_types_by_jurisdiction")
public class TaskTypeProviderTest extends SpringBootContractProviderBaseTest {

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
        testTarget.setControllers(new TaskTypesController(
            accessControlService,
            taskTypesService
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

    @State({"retrieve all task types by jurisdiction"})
    public void taskTypesByJurisdiction() {

        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(taskTypesService.getTaskTypes(any(), anyString()))
            .thenReturn(createTaskTypeResponse());
    }

    private GetTaskTypesResponse createTaskTypeResponse() {
        TaskType taskType = new TaskType("someTaskTypeId", "Some task type name");

        TaskTypeResponse taskTypeResponse = new TaskTypeResponse(taskType);

        return new GetTaskTypesResponse(List.of(taskTypeResponse));
    }

}
