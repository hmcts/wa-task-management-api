package uk.gov.hmcts.reform.wataskmanagementapi.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Provider("wa_task_management_api_complete_task_by_id")
//Uncomment this and comment the @PacBroker line to test TaskManagerClaimTaskConsumerTest local consumer.
@PactFolder("pacts")
//@PactBroker(
//    scheme = "${PACT_BROKER_SCHEME:http}",
//    host = "${PACT_BROKER_URL:localhost}",
//    port = "${PACT_BROKER_PORT:9292}",
//    consumerVersionSelectors = {
//        @VersionSelector(tag = "master")}
//)
@Import(TaskManagementProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
public class TaskManagerCompleteTaskProviderTest {

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private TaskManagementService taskManagementService;

    @Autowired
    private SystemDateProvider systemDateProvider;

    @Mock
    private ClientAccessControlService clientAccessControlService;

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
        testTarget.setControllers(new TaskActionsController(
            taskManagementService,
            accessControlService,
            systemDateProvider,
            clientAccessControlService
        ));
        if (context != null) {
            context.setTarget(testTarget);
        }
        testTarget.setMessageConverters((
            new MappingJackson2HttpMessageConverter(
                objectMapper
            )));    }

    @State({"complete a task using taskId"})
    public void completeTaskById() {
        setInitMockWithoutPrivilegedAccess();
    }

    @State({"complete a task using taskId and assign and complete completion options"})
    public void completeTaskByIdWithCompletionOptions() {
        setInitMockWithPrivilegedAccess();
    }

    private void setInitMockWithoutPrivilegedAccess() {
        doNothing().when(taskManagementService).completeTask(any(), any());
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(clientAccessControlService.hasPrivilegedAccess(any(), any())).thenReturn(false);
    }

    private void setInitMockWithPrivilegedAccess() {
        doNothing().when(taskManagementService).completeTaskWithPrivilegeAndCompletionOptions(any(), any(),any());
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(clientAccessControlService.hasPrivilegedAccess(any(), any())).thenReturn(true);
    }
}
