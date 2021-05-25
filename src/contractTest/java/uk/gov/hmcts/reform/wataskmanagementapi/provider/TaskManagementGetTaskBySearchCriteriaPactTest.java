package uk.gov.hmcts.reform.wataskmanagementapi.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskController;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@Provider("wa_task_management_api_search")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:9292}", consumerVersionSelectors = {
    @VersionSelector(tag = "latest")})
@Import(TaskManagementProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
//@PactFolder("pacts")
public class TaskManagementGetTaskBySearchCriteriaPactTest {

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private CamundaService camundaService;

    @Autowired
    private ObjectMapper objectMapper;

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

        testTarget.setMessageConverters((
            new MappingJackson2HttpMessageConverter(
                objectMapper
            )));

    }

    @State({"appropriate tasks are returned by criteria"})
    public void getTaskByCriteria() {
        setInitMockForsearchTask();
    }

    private void setInitMockForsearchTask() {
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(camundaService.searchWithCriteria(
            any(), anyInt(), anyInt(), any(), any())).thenReturn(createTasks()
        );
    }

    public List<Task> createTasks() {
        var tasks = new ArrayList<Task>();
        var taskOne =  new Task("4d4b6fgh-c91f-433f-92ac-e456ae34f72a",
                                              "Jake",
                                              "ReviewTheAppeal",
                                              "unconfigured",
                                              "SELF",
                                              "PRIVATE",
                                              "task name",
                                              ZonedDateTime.now(),
                                              ZonedDateTime.now(),
                                              "Mark Alistair",
                                              true,
                                              "Time extension",
                                              "IA",
                                              "1",
                                              "765324",
                                              "Newcastle",
                                              "Asylum",
                                              "4d4b3a4e-c91f-433f-92ac-e456ae34f72a",
                                              "processApplication",
                                              "Bob Smith",
                                              true);

        var taskTwo =  new Task("4d4b6fgh-cc1f-433f-92ac-e456aed4f72a",
                                              "Megan",
                                              "ReviewTheAppeal",
                                              "unconfigured",
                                              "SELF",
                                              "PRIVATE",
                                              "task name",
                                              ZonedDateTime.now(),
                                              ZonedDateTime.now(),
                                              "Jean Pierre",
                                              true,
                                              "Time extension",
                                              "IA",
                                              "1",
                                              "766524",
                                              "Newcastle",
                                              "Asylum",
                                              "4d4b3a4e-c9df-43sf-92ac-e456ee34fe2a",
                                              "processApplication",
                                              "Bob Smith",
                                              true);

        tasks.add(taskOne);
        tasks.add(taskTwo);

        return tasks;
    }



}

