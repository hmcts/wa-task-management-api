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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskSearchController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@Provider("wa_task_management_api_search_completable")
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
public class TaskManagementGetTaskBySearchForCompletablePactTest {

    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private TaskManagementService taskManagementService;
    @Mock
    private CftQueryService cftQueryService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SystemDateProvider systemDateProvider;

    @State({"appropriate tasks are returned by search for completable"})
    public void getTasksBySearchForCompletableCriteria() {
        setInitMockForSearchByCompletableTask();
    }

    @State({"appropriate tasks are returned by search for completable with warnings"})
    public void getTasksBySearchForCompletableCriteriaWithWarnings() {
        setInitMockForSearchByCompletableTaskWithWarnings();
    }

    public List<Task> createTasks() {
        Task task = new Task(
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
            true,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245335311",
            "refusalOfHumanRights",
            "Bob Smith",
            true,
            new WarningValues(emptyList()),
            "Some Case Management Category",
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
            RoleCategory.LEGAL_OPERATIONS.name(),
            "a description",
            null);

        return singletonList(task);
    }

    public List<Task> createTasksWithWarnings() {
        final List<Warning> warnings = List.of(
            new Warning("Code1", "Text1")
        );
        WarningValues warningValues = new WarningValues(warnings);
        Task taskWithWarnings = new Task(
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
            true,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245335311",
            "refusalOfHumanRights",
            "Bob Smith",
            true,
            warningValues,
            "Some Case Management Category",
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
            RoleCategory.LEGAL_OPERATIONS.name(),
            "a description",
            null);

        return singletonList(taskWithWarnings);
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
        testTarget.setControllers(new TaskSearchController(
            taskManagementService,
            accessControlService,
            cftQueryService,
            launchDarklyFeatureFlagProvider,
            systemDateProvider
        ));

        if (context != null) {
            context.setTarget(testTarget);
        }

        testTarget.setMessageConverters((
            new MappingJackson2HttpMessageConverter(
                objectMapper
            )));

    }

    private void setInitMockForSearchByCompletableTask() {
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(userInfo.getEmail()).thenReturn("test@test.com");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_TASK_QUERY, accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail())
        ).thenReturn(false);

        when(taskManagementService.searchForCompletableTasks(any(), any()))
            .thenReturn(new GetTasksCompletableResponse<>(false, createTasks()));
    }

    private void setInitMockForSearchByCompletableTaskWithWarnings() {
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(userInfo.getEmail()).thenReturn("test@test.com");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_TASK_QUERY, accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail())
        ).thenReturn(false);

        when(taskManagementService.searchForCompletableTasks(any(), any()))
            .thenReturn(new GetTasksCompletableResponse<>(false, createTasksWithWarnings()));
    }

}

