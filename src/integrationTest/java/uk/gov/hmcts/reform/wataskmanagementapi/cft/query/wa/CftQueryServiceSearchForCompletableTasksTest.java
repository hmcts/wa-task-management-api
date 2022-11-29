package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.wa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN_CLAIM;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AllowedJurisdictionConfiguration.class)
@Testcontainers
@Sql("/scripts/wa/search_for_completable_tasks_data.sql")
public class CftQueryServiceSearchForCompletableTasksTest extends RoleAssignmentHelper {

    private PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
        .buildSingleRequirementWithOr(OWN, EXECUTE);

    @MockBean
    private CamundaService camundaService;

    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    private CftQueryService cftQueryService;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, new TaskResourceDao(entityManager),
                                              allowedJurisdictionConfiguration, launchDarklyFeatureFlagProvider
        );
    }

    @Test
    void should_retrieve_a_task_grant_type_standard() {
        final String caseId = "1652446087857201";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);


        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );
        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponse());

        when(camundaService.getVariableValue(any(), any())).thenReturn("processApplication");

        final GetTasksCompletableResponse<Task> task = cftQueryService.searchForCompletableTasks(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            false
        );
        assertThat(task).isNotNull();
        assertThat(task.isTaskRequiredForEvent()).isTrue();
        assertThat(task.getTasks().get(0).getCaseId()).isEqualTo(caseId);
        assertThat(task.getTasks().get(0).getPermissions().getValues().containsAll(
            List.of(READ, OWN, EXECUTE))).isTrue();

    }

    @Test
    void should_retrieve_a_task_grant_type_standard_granular_permission() {
        final String caseId = "1652446087857201";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);


        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );
        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponse());

        when(camundaService.getVariableValue(any(), any())).thenReturn("processApplication");

        final GetTasksCompletableResponse<Task> task = cftQueryService.searchForCompletableTasks(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            true
        );
        assertThat(task).isNotNull();
        assertThat(task.isTaskRequiredForEvent()).isTrue();
        assertThat(task.getTasks().get(0).getCaseId()).isEqualTo(caseId);
        assertThat(task.getTasks().get(0).getPermissions().getValues().containsAll(
            List.of(READ, OWN, EXECUTE, UNASSIGN_CLAIM, UNASSIGN_ASSIGN))).isTrue();

    }

    @Test
    void should_not_retrieve_a_task_grant_type_standard_and_excluded() {
        final String caseId = "1652446087857201";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        //excluded
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);


        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );
        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponse());

        when(camundaService.getVariableValue(any(), any())).thenReturn("processApplication");

        final GetTasksCompletableResponse<Task> task = cftQueryService.searchForCompletableTasks(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            false
        );
        assertThat(task).isNotNull();
        assertThat(task.isTaskRequiredForEvent()).isTrue();
        assertThat(task.getTasks().size()).isEqualTo(0);

    }

    @Test
    void should_retrieve_a_task_grant_type_challenged() {
        final String caseId = "1652446087857202";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);


        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );
        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponse());

        when(camundaService.getVariableValue(any(), any())).thenReturn("processApplication");

        final GetTasksCompletableResponse<Task> task = cftQueryService.searchForCompletableTasks(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            false
        );
        assertThat(task).isNotNull();
        assertThat(task.isTaskRequiredForEvent()).isTrue();
        assertThat(task.getTasks().get(0).getCaseId()).isEqualTo(caseId);

    }

    @Test
    void should_not_retrieve_a_task_grant_type_challenged_and_excluded() {
        final String caseId = "1652446087857202";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        //excluded
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);


        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );
        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponse());

        when(camundaService.getVariableValue(any(), any())).thenReturn("processApplication");

        final GetTasksCompletableResponse<Task> task = cftQueryService.searchForCompletableTasks(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            false
        );
        assertThat(task).isNotNull();
        assertThat(task.isTaskRequiredForEvent()).isTrue();
        assertThat(task.getTasks().size()).isEqualTo(0);

    }

    @Test
    void should_retrieve_a_task_grant_type_specific() {
        final String caseId = "1652446087857203";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);


        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );
        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponse());

        when(camundaService.getVariableValue(any(), any())).thenReturn("processApplication");

        final GetTasksCompletableResponse<Task> task = cftQueryService.searchForCompletableTasks(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            false
        );
        assertThat(task).isNotNull();
        assertThat(task.isTaskRequiredForEvent()).isTrue();
        assertThat(task.getTasks().get(0).getCaseId()).isEqualTo(caseId);

    }

    @Test
    void should_retrieve_a_task_grant_type_specific_and_excluded() {
        final String caseId = "1652446087857203";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        //excluded
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);


        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );
        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponse());

        when(camundaService.getVariableValue(any(), any())).thenReturn("processApplication");

        final GetTasksCompletableResponse<Task> task = cftQueryService.searchForCompletableTasks(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            false
        );
        assertThat(task).isNotNull();
        assertThat(task.isTaskRequiredForEvent()).isTrue();
        assertThat(task.getTasks().get(0).getCaseId()).isEqualTo(caseId);
        assertThat(task.getTasks().get(0).getPermissions().getValues().containsAll(
            List.of(READ, OWN, EXECUTE))).isTrue();

    }

    private List<Map<String, CamundaVariable>> mockTaskCompletionDMNResponse() {
        List<Map<String, CamundaVariable>> dmnResult = new ArrayList<>();
        Map<String, CamundaVariable> response = Map.of(
            "completionMode", new CamundaVariable("Auto", "String"),
            "taskType", new CamundaVariable("processApplication", "String")
        );
        dmnResult.add(response);
        return dmnResult;
    }

}
