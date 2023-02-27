package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.wa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SECONDARY_IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SECONDARY_IDAM_USER_ID;


@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AllowedJurisdictionConfiguration.class)
@Testcontainers
@Sql("/scripts/wa/assign_task_data.sql")
public class CftQueryServiceAssignTaskTest extends RoleAssignmentHelper {

    @Mock
    private UserInfo assignerUserInfo;
    @Mock
    private UserInfo assigneeUserInfo;
    @MockBean
    private CamundaService camundaService;
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @Autowired
    private EntityManager entityManager;

    private CftQueryService cftQueryService;
    private ServiceMocks mockServices;
    private final List<PermissionTypes> assignerPermissionsRequired = singletonList(MANAGE);
    private final List<PermissionTypes> assigneePermissionsRequired = List.of(OWN, EXECUTE);

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, new TaskResourceDao(entityManager),
                                              allowedJurisdictionConfiguration
        );


        when(assignerUserInfo.getUid())
            .thenReturn(IDAM_USER_ID);
        when(assignerUserInfo.getEmail())
            .thenReturn(IDAM_USER_EMAIL);

        when(assigneeUserInfo.getUid())
            .thenReturn(SECONDARY_IDAM_USER_ID);
        when(assigneeUserInfo.getEmail())
            .thenReturn(SECONDARY_IDAM_USER_EMAIL);

        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
        mockServices.mockUserInfo();
        mockServices.mockSecondaryUserInfo();
    }

    @Test
    void should_retrieve_a_task_when_grant_type_standard() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111001";
        final String caseId = "1623278362431001";

        //assigner
        List<RoleAssignment> assignerRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
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

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        final Optional<TaskResource> assignerTaskResponse = cftQueryService.getTask(
            taskId,
            assignerRoleAssignments,
            assignerPermissionsRequired
        );

        Assertions.assertThat(assignerTaskResponse.isPresent()).isTrue();
        Assertions.assertThat(assignerTaskResponse.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(assignerTaskResponse.get().getCaseId()).isEqualTo(caseId);

        //assignee
        List<RoleAssignment> assigneeRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
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

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        final Optional<TaskResource> assigneeTaskResponse = cftQueryService.getTask(
            taskId,
            assigneeRoleAssignments,
            assigneePermissionsRequired
        );

        Assertions.assertThat(assigneeTaskResponse.isPresent()).isTrue();
        Assertions.assertThat(assigneeTaskResponse.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(assigneeTaskResponse.get().getCaseId()).isEqualTo(caseId);
    }

    @Test
    void should_not_retrieve_a_task_when_grant_type_standard_with_excluded() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111001";
        final String caseId = "1623278362431001";

        //assigner
        List<RoleAssignment> assignerRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
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

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        //exclude
        assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        final Optional<TaskResource> assignerTaskResponse = cftQueryService.getTask(
            taskId,
            assignerRoleAssignments,
            assignerPermissionsRequired
        );

        Assertions.assertThat(assignerTaskResponse.isEmpty()).isTrue();

        //assignee
        List<RoleAssignment> assigneeRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
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

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        //exclude
        assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        final Optional<TaskResource> assigneeTaskResponse = cftQueryService.getTask(
            taskId,
            assigneeRoleAssignments,
            assigneePermissionsRequired
        );

        Assertions.assertThat(assigneeTaskResponse.isEmpty()).isTrue();

    }

    @Test
    void should_retrieve_a_task_when_grant_type_challenged() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111002";
        final String caseId = "1623278362431002";
        //assigner
        List<RoleAssignment> assignerRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        final Optional<TaskResource> assignerTaskResponse = cftQueryService.getTask(
            taskId,
            assignerRoleAssignments,
            assignerPermissionsRequired
        );

        Assertions.assertThat(assignerTaskResponse.isPresent()).isTrue();
        Assertions.assertThat(assignerTaskResponse.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(assignerTaskResponse.get().getCaseId()).isEqualTo(caseId);

        //assignee
        List<RoleAssignment> assigneeRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        final Optional<TaskResource> assigneeTaskResponse = cftQueryService.getTask(
            taskId,
            assigneeRoleAssignments,
            assigneePermissionsRequired
        );

        Assertions.assertThat(assigneeTaskResponse.isPresent()).isTrue();
        Assertions.assertThat(assigneeTaskResponse.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(assigneeTaskResponse.get().getCaseId()).isEqualTo(caseId);


    }

    @Test
    void should_not_retrieve_a_task_when_challenged_with_excluded() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111002";
        final String caseId = "1623278362431002";

        //assigner
        List<RoleAssignment> assignerRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        //exclude
        assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        final Optional<TaskResource> assignerTaskResponse = cftQueryService.getTask(
            taskId,
            assignerRoleAssignments,
            assignerPermissionsRequired
        );

        Assertions.assertThat(assignerTaskResponse.isEmpty()).isTrue();

        //assignee
        List<RoleAssignment> assigneeRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        //exclude
        assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        final Optional<TaskResource> assigneeTaskResponse = cftQueryService.getTask(
            taskId,
            assigneeRoleAssignments,
            assigneePermissionsRequired
        );

        Assertions.assertThat(assigneeTaskResponse.isEmpty()).isTrue();

    }

    @Test
    void should_retrieve_a_task_when_grant_type_specific() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111003";
        final String caseId = "1623278362431003";

        //assigner
        List<RoleAssignment> assignerRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        final Optional<TaskResource> assignerTaskResponse = cftQueryService.getTask(
            taskId,
            assignerRoleAssignments,
            assignerPermissionsRequired
        );

        Assertions.assertThat(assignerTaskResponse.isPresent()).isTrue();
        Assertions.assertThat(assignerTaskResponse.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(assignerTaskResponse.get().getCaseId()).isEqualTo(caseId);

        //assignee own permission
        List<RoleAssignment> assigneeRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_CASE_MANAGER)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        final Optional<TaskResource> ownAssigneeTaskResponse = cftQueryService.getTask(
            taskId,
            assigneeRoleAssignments,
            assigneePermissionsRequired
        );

        Assertions.assertThat(ownAssigneeTaskResponse.isPresent()).isTrue();
        Assertions.assertThat(ownAssigneeTaskResponse.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(ownAssigneeTaskResponse.get().getCaseId()).isEqualTo(caseId);

        //assignee execute permission
        assigneeRoleAssignments = new ArrayList<>();

        assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        final Optional<TaskResource> executeAssigneeTaskResponse = cftQueryService.getTask(
            taskId,
            assigneeRoleAssignments,
            assigneePermissionsRequired
        );

        Assertions.assertThat(executeAssigneeTaskResponse.isPresent()).isTrue();
        Assertions.assertThat(executeAssigneeTaskResponse.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(executeAssigneeTaskResponse.get().getCaseId()).isEqualTo(caseId);

    }

    @Test
    void should_retrieve_a_task_when_grant_type_specific_with_excluded() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111003";
        final String caseId = "1623278362431003";

        //assigner
        List<RoleAssignment> assignerRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        //exclude
        assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        final Optional<TaskResource> assignerTaskResponse = cftQueryService.getTask(
            taskId,
            assignerRoleAssignments,
            assignerPermissionsRequired
        );

        Assertions.assertThat(assignerTaskResponse.isPresent()).isTrue();
        Assertions.assertThat(assignerTaskResponse.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(assignerTaskResponse.get().getCaseId()).isEqualTo(caseId);

        //assignee
        List<RoleAssignment> assigneeRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        //exclude
        assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        final Optional<TaskResource> assigneeTaskResponse = cftQueryService.getTask(
            taskId,
            assigneeRoleAssignments,
            assigneePermissionsRequired
        );

        Assertions.assertThat(assigneeTaskResponse.isPresent()).isTrue();
        Assertions.assertThat(assigneeTaskResponse.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(assigneeTaskResponse.get().getCaseId()).isEqualTo(caseId);

    }

    @Test
    void should_not_retrieve_a_task_when_grant_type_challenged_without_assigner_assignee_having_required_permission() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111005";
        final String caseId = "1623278362431005";

        //assigner
        List<RoleAssignment> assignerRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        final Optional<TaskResource> assignerTaskResponse = cftQueryService.getTask(
            taskId,
            assignerRoleAssignments,
            assignerPermissionsRequired
        );

        Assertions.assertThat(assignerTaskResponse.isEmpty()).isTrue();

        //assignee
        List<RoleAssignment> assigneeRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);


        final Optional<TaskResource> assigneeTaskResponse = cftQueryService.getTask(
            taskId,
            assigneeRoleAssignments,
            assigneePermissionsRequired
        );

        Assertions.assertThat(assigneeTaskResponse.isEmpty()).isTrue();

    }

    @Test
    void should_not_retrieve_a_task_when_grant_type_standard_without_assigner_assignee_having_required_permission() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111004";
        final String caseId = "1623278362431004";

        //assigner
        List<RoleAssignment> assignerRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
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

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        final Optional<TaskResource> assignerTaskResponse = cftQueryService.getTask(
            taskId,
            assignerRoleAssignments,
            assignerPermissionsRequired
        );

        Assertions.assertThat(assignerTaskResponse.isEmpty()).isTrue();

        //assignee
        List<RoleAssignment> assigneeRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
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

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        final Optional<TaskResource> assigneeTaskResponse = cftQueryService.getTask(
            taskId,
            assigneeRoleAssignments,
            assigneePermissionsRequired
        );

        Assertions.assertThat(assigneeTaskResponse.isEmpty()).isTrue();

    }

    @Test
    void should_not_retrieve_a_task_when_grant_type_specific_without_assigner_assignee_having_required_permission() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111006";
        final String caseId = "1623278362431006";

        //assigner
        List<RoleAssignment> assignerRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assignerRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoleAssignments, assignerRoleAssignmentRequest);

        final Optional<TaskResource> assignerTaskResponse = cftQueryService.getTask(
            taskId,
            assignerRoleAssignments,
            assignerPermissionsRequired
        );

        Assertions.assertThat(assignerTaskResponse.isEmpty()).isTrue();

        //assignee
        List<RoleAssignment> assigneeRoleAssignments = new ArrayList<>();

        RoleAssignmentRequest assigneeRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_CASE_MANAGER)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoleAssignments, assigneeRoleAssignmentRequest);

        final Optional<TaskResource> ownAssigneeTaskResponse = cftQueryService.getTask(
            taskId,
            assigneeRoleAssignments,
            assigneePermissionsRequired
        );

        Assertions.assertThat(ownAssigneeTaskResponse.isEmpty()).isTrue();
    }


}
