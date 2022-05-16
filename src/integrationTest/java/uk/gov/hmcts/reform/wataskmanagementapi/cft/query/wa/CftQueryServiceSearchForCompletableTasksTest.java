package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.wa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/search_for_completable_tasks_data.sql")
public class CftQueryServiceSearchForCompletableTasksTest extends RoleAssignmentHelper {

    private final List<PermissionTypes> permissionsRequired = new ArrayList<>();

    @MockBean
    private CamundaService camundaService;

    @Autowired
    private TaskResourceRepository taskResourceRepository;

    private CftQueryService cftQueryService;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, taskResourceRepository);
    }

    @Test
    void should_retrieve_a_task_grant_type_standard() {
        final String caseId = "1652446087857298";

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

        permissionsRequired.add(PermissionTypes.OWN);
        permissionsRequired.add(PermissionTypes.EXECUTE);

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );
        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponse());

        when(camundaService.getVariableValue(any(), any())).thenReturn("processApplication");

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        final GetTasksCompletableResponse<Task> task = cftQueryService.searchForCompletableTasks(searchEventAndCase,
            accessControlResponse, permissionsRequired);
        Assertions.assertThat(task).isNotNull();
        Assertions.assertThat(task.isTaskRequiredForEvent()).isTrue();
        Assertions.assertThat(task.getTasks().get(0).getCaseId()).isEqualTo(caseId);

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
