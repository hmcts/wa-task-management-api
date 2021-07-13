package uk.gov.hmcts.reform.wataskmanagementapi.provider;

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
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.privilege.PrivilegedAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Provider("wa_task_management_api_assign_task_by_id")
//Uncomment this and comment the @PacBroker line to test TaskManagerClaimTaskConsumerTest local consumer.
//@PactFolder("pacts")
@PactBroker(
    scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}",
    port = "${PACT_BROKER_PORT:9292}",
    consumerVersionSelectors = {
        @VersionSelector(tag = "master")}
)
@Import(TaskManagementProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
public class TaskManagerAssignTaskProviderTest {

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CamundaService camundaService;

    @Autowired
    private SystemDateProvider systemDateProvider;

    @Mock
    private PrivilegedAccessControlService privilegedAccessControlService;

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
            camundaService,
            accessControlService,
            systemDateProvider,
            privilegedAccessControlService
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
        doNothing().when(camundaService).assignTask(any(), any(), any(), any(), any());
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
    }
}
