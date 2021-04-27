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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskController;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@ExtendWith(SpringExtension.class)
@Provider("wa_task_management_api_claim_task_by_id")
//Uncomment this and comment the @PacBroker line to test TaskManagerClaimTaskConsumerTest local consumer.
//@PactFolder("target/pacts")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:https}",
    host = "${PACT_BROKER_URL:pact-broker.platform.hmcts.net}",
    port = "${PACT_BROKER_PORT:443}", consumerVersionSelectors = {
    @VersionSelector(tag = "latest")})
@Import(TaskManagementProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
public class TaskManagerClaimTaskProviderTest {

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

    @State({"claim a task using taskId"})
    public void claimTaskById() {
        setInitMock();
    }

    private void setInitMock() {
        doNothing().when(camundaService).claimTask(any(),any(),any());
    }
}
