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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/get_task_data.sql")
public class CftQueryServiceGetTaskTest extends RoleAssignmentHelper {

    private List<PermissionTypes> permissionsRequired = new ArrayList<>();

    @MockBean
    private CamundaService camundaService;
    @Autowired
    private TaskResourceRepository taskResourceRepository;

    private CftQueryService cftQueryService;

    @BeforeEach
    void setUp() {
        permissionsRequired = singletonList(PermissionTypes.READ);
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, taskResourceRepository);
    }

    @Test
    void should_return_empty_task_resource_when_task_id_is_null() {
        final String taskId = null;
        final String caseId = "1623278362431001";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, accessControlResponse, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    @Test
    void should_return_empty_task_resource_when_task_id_is_empty() {
        final String taskId = "";
        final String caseId = "1623278362431001";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, accessControlResponse, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    @Test
    void should_return_empty_task_resource_when_permissions_are_missing() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111001";
        final String caseId = "1623278362431001";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired = emptyList();
        final Optional<TaskResource> task = cftQueryService.getTask(taskId, accessControlResponse, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    @Test
    void should_return_empty_task_resource_when_permissions_are_wrong() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111001";
        final String caseId = "1623278362431001";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired = singletonList(PermissionTypes.OWN);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, accessControlResponse, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    @Test
    void should_return_task_resource_when_authorisations_are_correct() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111004";
        final String caseId = "1623278362431004";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .authorisations(List.of("DIVORCE", "373"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, accessControlResponse, permissionsRequired);
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);

    }

    @Test
    void should_return_empty_task_resource_when_authorisations_are_not_fully_matched() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111004";
        final String caseId = "1623278362431004";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .authorisations(singletonList("373"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, accessControlResponse, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isTrue();

    }

    @Test
    void should_return_empty_task_resource_when_authorisations_are_not_matched() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111004";
        final String caseId = "1623278362431004";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .authorisations(List.of("PROBATE", "SCSS"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, accessControlResponse, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isTrue();

    }

    @Test
    void should_return_empty_task_resource_when_authorisations_is_empty() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111004";
        final String caseId = "1623278362431004";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .authorisations(emptyList())
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, accessControlResponse, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

}
