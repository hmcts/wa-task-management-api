package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.wa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AllowedJurisdictionConfiguration.class)
@Testcontainers
@Sql("/scripts/wa/get_task_data.sql")
public class CftQueryServiceGetTaskTest extends RoleAssignmentHelper {

    private List<PermissionTypes> permissionsRequired = new ArrayList<>();

    @MockBean
    private CamundaService camundaService;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    private CftQueryService cftQueryService;

    @BeforeEach
    void setUp() {
        permissionsRequired = singletonList(PermissionTypes.READ);
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, new TaskResourceDao(entityManager),
                                              allowedJurisdictionConfiguration
        );
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

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
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

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
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

        permissionsRequired = emptyList();
        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
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

        permissionsRequired = singletonList(PermissionTypes.OWN);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
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

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);

    }

    @Test
    void should_return_task_resource_when_location_and_region_attribute_match() {
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
                    .baseLocation("765324")
                    .build()
            )
            .authorisations(List.of("DIVORCE", "373"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);

    }

    @Test
    void should_return_task_resource_when_authorisations_are_partially_matched() {
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

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isFalse();

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

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
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

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

}
