package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.config.executors.ExecutorServiceConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.SensitiveTaskEventLogsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.IA_JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.PRIMARY_LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.WA_JURISDICTION;

@Slf4j
@ActiveProfiles("integration")
@DataJpaTest
@Import(ExecutorServiceConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/get_task_data.sql")
public class CFTSensitiveTaskEventLogsDatabaseServiceTest {

    @Autowired
    TaskResourceRepository taskResourceRepository;
    @Autowired
    SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;
    @Autowired
    ExecutorService sensitiveTaskEventLogsExecutorService;
    CFTTaskDatabaseService cftTaskDatabaseService;
    CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;
    static RoleAssignmentHelper roleAssignmentHelper = new RoleAssignmentHelper();

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository, cftTaskMapper);
        cftSensitiveTaskEventLogsDatabaseService = new CFTSensitiveTaskEventLogsDatabaseService(
            sensitiveTaskEventLogsRepository,
            cftTaskDatabaseService,
            sensitiveTaskEventLogsExecutorService
        );
    }

    @Test
    @Transactional
    void should_process_and_save_sensitive_task_event_log() throws InterruptedException {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();

        String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111001";

        cftSensitiveTaskEventLogsDatabaseService.processSensitiveTaskEventLog(
            taskId,
            roleAssignments,
            ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE
        );

        sensitiveTaskEventLogsExecutorService.awaitTermination(400, TimeUnit.MILLISECONDS);

        Optional<SensitiveTaskEventLog> sensitiveTaskEventLog = sensitiveTaskEventLogsRepository.getByTaskId(taskId);
        assertThat(sensitiveTaskEventLog).isPresent();
        assertThat(sensitiveTaskEventLog.get().getTaskData()).isNotEmpty().hasSize(1);
        assertThat(sensitiveTaskEventLog.get().getUserData()).isNotNull();
    }

    private static List<RoleAssignment> roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + Classification.PUBLIC.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);


        return roleAssignments;
    }
}
