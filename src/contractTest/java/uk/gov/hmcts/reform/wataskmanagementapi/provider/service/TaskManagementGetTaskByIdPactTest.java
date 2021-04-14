package uk.gov.hmcts.reform.wataskmanagementapi.provider.service;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskController;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.ZonedDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@Provider("task_management_get_task_by_id")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}", host = "${PACT_BROKER_URL:localhost}")
@Import(TaskManagementProviderTestConfiguration.class)
public class TaskManagementGetTaskByIdPactTest {

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private CamundaService camundaService;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        testTarget.setControllers(new TaskController(
            camundaService,
            accessControlService
        ));
        context.setTarget(testTarget);

    }

    @State({"will return a task by task id."})
    public void getTaskById() {
        setInitiMock();
    }

    private void setInitiMock() {
        when(camundaService.getTask(any(),any(),any())).thenReturn(createTask());
    }

    public Task createTask() {
        return new Task("id",
                         "Jake",
                         "ReviewTheAppeal",
                         "unconfigured",
                         "taskSystem",
                         "PRIVATE",
                         "review",
                         ZonedDateTime.now(),
                         ZonedDateTime.now(),
                         "TCW",
                         true,
                         "Time extension",
                         "ia",
                         "region",
                         "12345",
                         "Newcastle",
                         "asylum",
                         "4d4b3a4e-c91f-433f-92ac-e456ae34f72a",
                         "processApplication",
                         "caseName",
                         true);

    }
}

