package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.SensitiveTaskEventLogsRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/scripts/wa/get_task_data.sql")
public class CFTSensitiveTaskEventLogsDatabaseServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;

    @Autowired
    ExecutorService sensitiveTaskEventLogsExecutorService;
    @Autowired
    CFTTaskDatabaseService cftTaskDatabaseService;
    CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;

    @BeforeEach
    void setUp() {
        cftSensitiveTaskEventLogsDatabaseService = new CFTSensitiveTaskEventLogsDatabaseService(
            sensitiveTaskEventLogsRepository,
            cftTaskDatabaseService,
            sensitiveTaskEventLogsExecutorService
        );
    }

    @Test
    @Transactional
    void should_process_and_save_sensitive_task_event_log() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();

        String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111001";

        cftSensitiveTaskEventLogsDatabaseService.processSensitiveTaskEventLog(
            taskId,
            roleAssignments,
            ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE
        );

        ExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        executorService.execute(() -> verify(sensitiveTaskEventLogsRepository, times(1))
            .save(any(SensitiveTaskEventLog.class)));

        Optional<SensitiveTaskEventLog> sensitiveTask = sensitiveTaskEventLogsRepository.findByTaskId(taskId);
        assertThat(sensitiveTask).isPresent();
        assertThat(sensitiveTask.get().getTaskData()).isNotEmpty().hasSize(1);    
    }

    private static List<RoleAssignment> roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + Classification.PUBLIC.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentHelper.RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + Classification.PUBLIC.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(IA_JURISDICTION)
                    .region("2")
                    .baseLocation("765325")
                    .build()
            )
            .authorisations(List.of("skill2"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + Classification.PRIVATE.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);


        return roleAssignments;
    }
}
