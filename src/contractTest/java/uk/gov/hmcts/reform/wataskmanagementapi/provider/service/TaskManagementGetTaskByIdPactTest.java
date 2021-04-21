package uk.gov.hmcts.reform.wataskmanagementapi.provider.service;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
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
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.InsufficientPermissionsException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.ZonedDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@Provider("wa_task_management_api_get_task_by_id")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:9292}", consumerVersionSelectors = {
    @VersionSelector(tag = "master")})
@Import(TaskManagementProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
public class TaskManagementGetTaskByIdPactTest {

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private CamundaService camundaService;

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
        testTarget.setControllers(new TaskController(
            camundaService,
            accessControlService
        ));
        if (context != null) {
            context.setTarget(testTarget);
        }

    }

    @State({"appropriate task is returned"})
    public void getTaskById() {
        setInitiMock();
    }

    @State({"returns 404 from a get task call"})
    public void responseError404Response() {
        setInitiMockPermissions();
    }

    @State({"returns 401 from a get task call"})
    public void responseError401Response() {
        setInitiMockPermissions();
    }

    private void setInitiMock() {
        when(camundaService.getTask(any(),any(),any())).thenReturn(createTask());
    }

    private void setInitiMockPermissions() {
        when(camundaService.getTask(any(),any(),any())).thenThrow(new InsufficientPermissionsException(
            "User did not have sufficient permissions to access task with id: 0000-0000-0000-0000")
        );
    }

    private void setInitiMockResources() {
        when(camundaService.getTask(any(),any(),any())).thenThrow(new ResourceNotFoundException(
            "There was a problem fetching the variables for task with id", null)
        );
    }

    public Task createTask() {
        return new Task("id",
                         "Jake",
                         "ReviewTheAppeal",
                         "unconfigured",
                         "main",
                         "PRIVATE",
                         "review",
                         ZonedDateTime.now(),
                         ZonedDateTime.now(),
                         "Mark Alistair",
                         true,
                         "Time extension",
                         "IA",
                         "South",
                         "12345",
                         "Newcastle",
                         "Asylum",
                         "4d4b3a4e-c91f-433f-92ac-e456ae34f72a",
                         "processApplication",
                         "caseName",
                         true);
    }



}

