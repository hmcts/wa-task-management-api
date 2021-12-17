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
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@Provider("wa_task_management_api_get_task_by_id")
//Uncomment this and comment the @PactBroker line to test TaskManagementGetTaskProviderTest local consumer.
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
public class TaskManagementGetTaskProviderTest {

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private TaskManagementService taskManagementService;

    @Autowired
    private SystemDateProvider systemDateProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private ClientAccessControlService clientAccessControlService;

    @State({"get a task using taskId"})
    public void getTaskById() {
        setInitMockTask();
    }

    @State({"get a task using taskId with warnings"})
    public void getTaskByIdWithWarnings() {
        setInitMockTaskWithWarnings();
    }

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

        testTarget.setMessageConverters(
            (
                new MappingJackson2HttpMessageConverter(objectMapper)
            )
        );

    }

    private void setInitMockTask() {
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(taskManagementService.getTask(any(), any())).thenReturn(createTask());
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
    }

    private void setInitMockTaskWithWarnings() {
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(taskManagementService.getTask(any(), any())).thenReturn(createTaskWithWarnings());
    }

    private Task createTask() {
        return new Task(
            "4d4b6fgh-c91f-433f-92ac-e456ae34f72a",
            "Review the appeal",
            "reviewTheAppeal",
            "assigned",
            "SELF",
            "PUBLIC",
            "Review the appeal",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "10bac6bf-80a7-4c81-b2db-516aba826be6",
            false,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245335311",
            "refusalOfHumanRights",
            "Bob Smith",
            false,
            new WarningValues(Collections.emptyList()),
            "Case Management Category",
            "hearing_work",
            new TaskPermissions(
                new HashSet<>(
                    asList(
                        PermissionTypes.READ,
                        PermissionTypes.OWN,
                        PermissionTypes.EXECUTE,
                        PermissionTypes.CANCEL,
                        PermissionTypes.MANAGE,
                        PermissionTypes.REFER
                    ))),
            "LEGAL_OPERATIONS",
            "a description"
        );
    }

    private Task createTaskWithWarnings() {
        final List<Warning> warnings = List.of(
            new Warning("Code1", "Text1")
        );
        WarningValues warningValues = new WarningValues(warnings);
        return new Task(
            "4d4b6fgh-c91f-433f-92ac-e456ae34f72a",
            "Review the appeal",
            "reviewTheAppeal",
            "assigned",
            "SELF",
            "PUBLIC",
            "Review the appeal",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "10bac6bf-80a7-4c81-b2db-516aba826be6",
            false,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245335311",
            "refusalOfHumanRights",
            "Bob Smith",
            false,
            warningValues,
            "Case Management Category",
            "hearing_work",
            new TaskPermissions(
                new HashSet<>(
                    asList(
                        PermissionTypes.READ,
                        PermissionTypes.OWN,
                        PermissionTypes.EXECUTE,
                        PermissionTypes.CANCEL,
                        PermissionTypes.MANAGE,
                        PermissionTypes.REFER
                    ))),
            "LEGAL_OPERATIONS",
            "a description"
        );
    }

}
