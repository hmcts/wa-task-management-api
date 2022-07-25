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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;

import static java.util.Collections.emptyList;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AllowedJurisdictionConfiguration.class)
@Testcontainers
@Sql("/scripts/authorise_user_data.sql")
public class CftQueryServiceAuthoriseUserTaskTest {

    private final List<PermissionTypes> permissionsRequired = new ArrayList<>();

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
    void should_get_a_task_to_user_with_divorce_and_iac_authorisation() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
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

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );
        permissionsRequired.add(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);
    }

    @Test
    void should_get_a_task_to_user_with_divorce_only_authorisation() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("DIVORCE"))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );
        permissionsRequired.add(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);
    }

    @Test
    void should_not_return_task_when_user_has_invalid_authorisation() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("NO Role"))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );
        permissionsRequired.add(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );
        Assertions.assertThat(task.isPresent()).isFalse();
    }

    @Test
    void should_not_return_task_when_user_has_no_authorisation() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .authorisations(emptyList())
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired.add(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );
        Assertions.assertThat(task.isPresent()).isFalse();
    }

    @Test
    void should_return_task_when_both_user_and_task_have_empty_role() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111019";
        final String caseId = "1623278362431019";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .authorisations(emptyList())
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired.add(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );
        Assertions.assertThat(task.isPresent()).isTrue();
    }

    @Test
    void should_get_a_task_when_user_has_more_authorisations_than_task() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111020";
        final String caseId = "1623278362431020";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
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

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );
        permissionsRequired.add(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);
    }

    @Test
    void should_get_a_task_when_user_has_same_authorisation_as_task() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111021";
        final String caseId = "1623278362431021";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("DIVORCE"))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );
        permissionsRequired.add(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);
    }
}
