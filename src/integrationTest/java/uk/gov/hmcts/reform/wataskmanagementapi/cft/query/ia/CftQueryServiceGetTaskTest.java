package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.LOCATION;

@ActiveProfiles("integration")
@DataJpaTest
@Import(AllowedJurisdictionConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/ia/get_task_data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CftQueryServiceGetTaskTest extends RoleAssignmentHelper {

    private final List<PermissionTypes> permissionsRequired = new ArrayList<>();

    @MockitoBean
    private CamundaService camundaService;
    @Autowired
    private TaskResourceRepository taskResourceRepository;

    private CftQueryService cftQueryService;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, new TaskResourceDao(entityManager),
                                              allowedJurisdictionConfiguration
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getGrantTypes")
    void should_get_a_task(GrantType grantType) {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111017";
        final String caseId = "1623278362431017";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        Map<String, String> tcAttributes = new HashMap<>();

        if (grantType == GrantType.SPECIFIC) {
            tcAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION);
            tcAttributes.put(RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE);
            tcAttributes.put(RoleAttributeDefinition.CASE_ID.value(), caseId);
        } else if (grantType == GrantType.STANDARD) {
            tcAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION);
            tcAttributes.put(RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE);
            tcAttributes.put(RoleAttributeDefinition.CASE_ID.value(), caseId);
            tcAttributes.put(LOCATION.name(), "1");
        }

        RoleAssignment roleAssignment = RoleAssignment
            .builder()
            .roleName("tribunal-caseworker")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(grantType)
            .attributes(tcAttributes)
            .authorisations(List.of("373"))
            .build();
        roleAssignments.add(roleAssignment);

        permissionsRequired.add(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);
    }

    @Test
    void should_get_a_task_to_with_challenged_grant_type() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("DIVORCE", "373"))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        permissionsRequired.add(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);
        Set<TaskRoleResource> taskRoleResourceSet = task.get().getTaskRoleResources();
        Assertions.assertThat(taskRoleResourceSet).isNotEmpty();
        taskRoleResourceSet.stream().forEach(taskRoleResource -> {
            assertArrayEquals(new String[]{"DIVORCE", "373"}, taskRoleResource.getAuthorizations());
            Assertions.assertThat(taskRoleResource.getAssignmentPriority()).isEqualTo(8);
            Assertions.assertThat(taskRoleResource.getAutoAssignable()).isTrue();
        });
    }

    @Test
    void should_return_empty_task_resource_when_authorisations_are_wrong_for_challenge_grant_type() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("PROBATE", "SCSS"))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired.add(PermissionTypes.OWN);
        permissionsRequired.add(PermissionTypes.EXECUTE);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    @Test
    void should_return_empty_task_resource_when_authorisations_is_empty_for_challenge_grant_type() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired.add(PermissionTypes.OWN);
        permissionsRequired.add(PermissionTypes.EXECUTE);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, permissionsRequired);
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    private static Stream<GrantType> getGrantTypes() {
        return Stream.of(GrantType.STANDARD, GrantType.SPECIFIC);
    }

}
