package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.wa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.EntityManager;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AllowedJurisdictionConfiguration.class)
@Testcontainers
@Sql("/scripts/wa/get_task_granular_permission_data.sql")
public class CftQueryServiceGetTaskWithGranularPermissionTest extends RoleAssignmentHelper {

    @MockBean
    private CamundaService camundaService;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    private CftQueryService cftQueryService;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, new TaskResourceDao(entityManager),
                                              allowedJurisdictionConfiguration
        );
    }

    @Test
    void should_return_task() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111004";
        final String caseId = "1623278362431004";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentHelper.RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentHelper
            .RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentHelper.RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .authorisations(singletonList("373"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, requirements);
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);

    }

    @ParameterizedTest
    @MethodSource({"getTaskId"})
    void should_return_task_for_granular_permission_required_for_claim(String taskId) {
        final String caseId = "1623278362431004";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentHelper.RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentHelper
            .RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentHelper.RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .authorisations(singletonList("373"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .initPermissionRequirement(asList(CLAIM, OWN), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(CLAIM, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(ASSIGN, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(ASSIGN, OWN), PermissionJoin.AND)
            .build();

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, requirements);
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);

    }

    @ParameterizedTest
    @MethodSource({"getExcludedTaskId"})
    void should_not_return_task_for_granular_permission_required_for_claim(String taskId) {
        final String caseId = "1623278362431004";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentHelper.RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentHelper
            .RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentHelper.RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .authorisations(singletonList("373"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .initPermissionRequirement(asList(CLAIM, OWN), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(CLAIM, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(ASSIGN, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(ASSIGN, OWN), PermissionJoin.AND)
            .build();

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, requirements);
        Assertions.assertThat(task.isPresent()).isFalse();

    }

    private static Stream<String> getTaskId() {
        return Stream.of("8d6cc5cf-c973-11eb-bdba-0242ac111005",
                         "8d6cc5cf-c973-11eb-bdba-0242ac111006",
                         "8d6cc5cf-c973-11eb-bdba-0242ac111007",
                         "8d6cc5cf-c973-11eb-bdba-0242ac111008");
    }

    private static Stream<String> getExcludedTaskId() {
        return Stream.of("8d6cc5cf-c973-11eb-bdba-0242ac111009",
                         "8d6cc5cf-c973-11eb-bdba-0242ac111010");
    }

}
