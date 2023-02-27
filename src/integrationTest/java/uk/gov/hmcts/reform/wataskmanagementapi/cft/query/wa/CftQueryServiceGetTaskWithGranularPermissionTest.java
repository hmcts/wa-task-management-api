package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.wa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
import java.util.stream.Stream;
import javax.persistence.EntityManager;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin.OR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN_CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNCLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNCLAIM_ASSIGN;

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
    @MethodSource({"getClaimTaskId"})
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
    @MethodSource({"getClaimExcludedTaskId"})
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

    @ParameterizedTest
    @MethodSource({"getAssignTaskId"})
    void should_return_task_for_granular_permission_required_for_assigner(String taskId,
                                                                          PermissionRequirements requirements) {
        final String caseId = "1623278362431005";

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

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, requirements);
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);

    }

    @ParameterizedTest
    @MethodSource({"getAssignExcludedTaskId"})
    void should_not_return_task_for_granular_permission_required_for_assign(String taskId,
                                                                           PermissionRequirements requirements) {
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

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, requirements);
        Assertions.assertThat(task.isPresent()).isFalse();
    }

    public static Stream<Arguments> getAssignTaskId() {
        return Stream.of(
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111011", PermissionRequirementBuilder.builder()
                .buildSingleType(ASSIGN)),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111011", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(CLAIM).build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111012", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(CLAIM).build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111013", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_CLAIM)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, CLAIM), PermissionJoin.AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111014", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_CLAIM)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, CLAIM), PermissionJoin.AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111015", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_CLAIM)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, CLAIM), PermissionJoin.AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111016", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_CLAIM)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, CLAIM), PermissionJoin.AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111017", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNCLAIM_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNCLAIM, ASSIGN), PermissionJoin.AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111018", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNCLAIM_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNCLAIM, ASSIGN), PermissionJoin.AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111015", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111016", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111019", PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(UNASSIGN, UNCLAIM))
        );
    }

    public static Stream<Arguments> getAssignExcludedTaskId() {
        return Stream.of(
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac115012", PermissionRequirementBuilder.builder()
                .buildSingleType(ASSIGN)),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac115013", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(CLAIM).build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111011", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_CLAIM)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, CLAIM), PermissionJoin.AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111011", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNCLAIM_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNCLAIM, ASSIGN), PermissionJoin.AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111011", PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND)
                .build()),
            Arguments.of("8d6cc5cf-c973-11eb-bdba-0242ac111011", PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(UNASSIGN, UNCLAIM))
        );
    }

    private static Stream<String> getClaimTaskId() {
        return Stream.of("8d6cc5cf-c973-11eb-bdba-0242ac111005",
                         "8d6cc5cf-c973-11eb-bdba-0242ac111006",
                         "8d6cc5cf-c973-11eb-bdba-0242ac111007",
                         "8d6cc5cf-c973-11eb-bdba-0242ac111008");
    }

    private static Stream<String> getClaimExcludedTaskId() {
        return Stream.of("8d6cc5cf-c973-11eb-bdba-0242ac111009",
                         "8d6cc5cf-c973-11eb-bdba-0242ac111010");
    }

}
