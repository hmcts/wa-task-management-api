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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Provider("wa_task_management_api_search")
//Uncomment this and comment the @PactBroker line to test WorkTypeConsumerTest local consumer.
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
public class TaskManagementGetTaskBySearchCriteriaPactTest {

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private TaskManagementService taskManagementService;

    @Mock
    private CftQueryService cftQueryService;

    @Mock
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @State({"appropriate tasks are returned by criteria"})
    public void getTasksBySearchCriteria() {
        setInitMockForSearchTask();
    }

    @State({"appropriate tasks are returned by criteria with work-type"})
    public void getTasksBySearchCriteriaWithWorkType() {
        setInitMockForSearchTask();
    }

    @State({"appropriate tasks are returned by criteria with no warnings"})
    public void getTasksBySearchCriteriaWithNoWarnings() {
        setInitMockForSearchTaskWithNoWarnings();
    }

    @State({"appropriate tasks are returned by criteria with warnings only"})
    public void getTasksBySearchCriteriaWithWarningsOnly() {
        setInitMockForSearchTaskWithWarningsOnly();
    }

    @State({"appropriate tasks are returned by criteria with work-type with warnings only"})
    public void getTasksBySearchCriteriaWithWorkTypeWithWarningsOnly() {
        setInitMockForSearchTaskWithWarningsOnly();
    }

    public Task createTaskWithNoWarnings() {
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
            "a description",
            RoleCategory.LEGAL_OPERATIONS.name()
        );
    }

    public Task createTaskWithWarnings() {
        final List<Warning> warnings = List.of(
            new Warning("Code1", "Text1")
        );

        return new Task(
            "fda422de-b381-43ff-94ea-eea5790188a3",
            "Review the appeal",
            "reviewTheAppeal",
            "unassigned",
            "SELF",
            "PUBLIC",
            "Review the appeal",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            null,
            true,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245308495",
            "refusalOfHumanRights",
            "John Doe",
            true,
            new WarningValues(warnings),
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
            "a description",
            RoleCategory.LEGAL_OPERATIONS.name()
        );
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
            launchDarklyFeatureFlagProvider
        ));

        if (context != null) {
            context.setTarget(testTarget);
        }

        testTarget.setMessageConverters((
            new MappingJackson2HttpMessageConverter(
                objectMapper
            )));

    }

    private void setInitMockForSearchTask() {
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

        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any()))
            .thenReturn(asList(createTaskWithNoWarnings(), createTaskWithNoWarnings()));
    }

    private void setInitMockForSearchTaskWithWarningsOnly() {
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_TASK_QUERY, accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail())
        ).thenReturn(false);
        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any()))
            .thenReturn(singletonList(createTaskWithWarnings()));
    }


    private void setInitMockForSearchTaskWithNoWarnings() {
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn("dummyUserId");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_TASK_QUERY, accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail())
        ).thenReturn(false);

        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any()))
            .thenReturn(singletonList(createTaskWithNoWarnings()));
    }

}
