package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.wa;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.WA_CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.WA_JURISDICTION;
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
@Transactional//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CftQueryServiceSearchForCompletableTasksTest {

    private PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
        .buildSingleRequirementWithOr(OWN, EXECUTE);

    @MockitoBean
    private CamundaService camundaService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    RoleAssignmentHelper roleAssignmentHelper = new RoleAssignmentHelper();

    private CftQueryService cftQueryService;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, new TaskResourceDao(entityManager),
                                              allowedJurisdictionConfiguration
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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);


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
            permissionsRequired
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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);


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
            permissionsRequired
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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);


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
            permissionsRequired
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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);


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
            permissionsRequired
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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);


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
            permissionsRequired
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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);


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
            permissionsRequired
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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);


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
            permissionsRequired
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
