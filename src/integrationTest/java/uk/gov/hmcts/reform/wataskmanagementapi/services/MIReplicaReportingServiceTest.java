package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReplicaTaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.db.MIReplicaDBDao;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskAssignmentsResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.replica.ReplicaTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.AwaitilityIntegrationTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ReplicaIntegrationTestUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.TERMINATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.ReplicaBaseTest.ENVIRONMENT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.ReplicaBaseTest.TEST_PRIMARY_DB_PASS;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.ReplicaBaseTest.TEST_PRIMARY_DB_USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.ReplicaBaseTest.TEST_PUBLICATION_URL;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.ReplicaBaseTest.TEST_REPLICA_DB_PASS;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.ReplicaBaseTest.TEST_REPLICA_DB_USER;

/**
 * We test logical replication in here.
 */
@ActiveProfiles("replica")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@Slf4j
@Import(AwaitilityIntegrationTestConfig.class)
class MIReplicaReportingServiceTest {

    @Autowired
    protected TaskResourceRepository taskResourceRepository;

    @Autowired
    protected TaskHistoryResourceRepository taskHistoryResourceRepository;
    @Autowired
    protected ReportableTaskRepository reportableTaskRepository;
    @Autowired
    protected TaskAssignmentsRepository taskAssignmentsRepository;
    @Autowired
    protected MIReportingService miReportingService;

    @Value("${spring.datasource.jdbcUrl}")
    protected String primaryJdbcUrl;

    @Value("${spring.datasource-replica.jdbcUrl}")
    private String replicaJdbcUrl;

    @Autowired
    protected ReplicaTaskResourceRepository replicaTaskResourceRepository;

    ReplicaIntegrationTestUtils replicaIntegrationTestUtils;

    @BeforeAll
    void init() {
        replicaIntegrationTestUtils = new ReplicaIntegrationTestUtils(
            taskResourceRepository,
            taskHistoryResourceRepository,
            reportableTaskRepository,
            taskAssignmentsRepository,
            miReportingService,
            primaryJdbcUrl,
            replicaJdbcUrl
        );
        replicaIntegrationTestUtils.setUp();
    }

    @AfterAll
    void tearDown() {
        replicaIntegrationTestUtils.tearDown();
    }

    @Test
    void should_save_task_and_get_task_from_replica_tables() {
        TaskResource taskResource = createAndSaveTask();

        await()
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByTaskId(taskResource.getTaskId());

                    assertFalse(taskHistoryResourceList.isEmpty());
                    assertEquals(1, taskHistoryResourceList.size());
                    assertEquals(taskResource.getTaskId(), taskHistoryResourceList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskHistoryResourceList.get(0).getTaskName());
                    assertEquals(taskResource.getTitle(), taskHistoryResourceList.get(0).getTaskTitle());
                    assertEquals(taskResource.getAssignee(), taskHistoryResourceList.get(0).getAssignee());
                    assertTrue(taskResource.getLastUpdatedTimestamp()
                        .isEqual(taskHistoryResourceList.get(0).getUpdated()));
                    assertEquals(taskResource.getState().toString(), taskHistoryResourceList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), taskHistoryResourceList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), taskHistoryResourceList.get(0).getUpdateAction());

                    return true;
                });
    }

    @Test
    void should_save_task_and_get_task_from_reportable_task() {
        TaskResource taskResource = createAndSaveTask();

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), reportableTaskList.get(0).getTaskName());
                    assertEquals(taskResource.getTitle(), reportableTaskList.get(0).getTaskTitle());
                    assertEquals(taskResource.getAssignee(), reportableTaskList.get(0).getAssignee());
                    assertTrue(taskResource.getLastUpdatedTimestamp().isEqual(reportableTaskList.get(0).getUpdated()));
                    assertEquals(taskResource.getState().toString(), reportableTaskList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), reportableTaskList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), reportableTaskList.get(0).getUpdateAction());

                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "ADD, false",
        "UPDATE, false",
        "ADD, true",
        "UPDATE, true"
    })
    void should_save_task_and_get_task_from_replica_tables_with_new_columns(String operation, boolean required) {
        TaskResource taskResource;
        if ("UPDATE".equals(operation)) {
            taskResource = buildTaskResource(3,5);
            taskResource = taskResourceRepository.save(taskResource);
            checkHistory(taskResource.getTaskId(), 1);
            log.info("Operation UPDATE and Check History Done with 1 record");
            taskResource.setState(CFTTaskState.COMPLETED);
            taskResource.setLastUpdatedAction("AutoAssign");
            taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        } else {
            taskResource = buildTaskResource(3,5);
        }

        addMissingParameters(taskResource, required);

        TaskResource savedTaskResource = taskResourceRepository.save(taskResource);
        log.info("Operation {} and saved taskResource", operation);

        await()
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByTaskId(savedTaskResource.getTaskId());

                    assertFalse(taskHistoryResourceList.isEmpty());
                    assertEquals("UPDATE".equals(operation) ? 2 : 1, taskHistoryResourceList.size());

                    TaskHistoryResource taskHistoryResource = "UPDATE".equals(operation)
                        ? taskHistoryResourceList.get(1) : taskHistoryResourceList.get(0);
                    assertEquals(savedTaskResource.getTaskId(), taskHistoryResource.getTaskId());
                    assertEquals(savedTaskResource.getDescription(), taskHistoryResource.getDescription(),
                                 "Description should match");
                    log.info("Description should match : {}, {}",
                             savedTaskResource.getDescription(), taskHistoryResource.getDescription());
                    assertEquals(savedTaskResource.getRegionName(), taskHistoryResource.getRegionName(),
                                 "RegionName should match");
                    log.info("RegionName should match : {}, {} ",
                             savedTaskResource.getRegionName(), taskHistoryResource.getRegionName());
                    assertEquals(savedTaskResource.getLocationName(), taskHistoryResource.getLocationName(),
                                 "LocationName should match");
                    log.info("LocationName should match : {}, {} ",
                             savedTaskResource.getLocationName(), taskHistoryResource.getLocationName());
                    assertEquals(savedTaskResource.getNotes(), taskHistoryResource.getNotes(),
                                 "Notes should match");
                    log.info("Notes should match : {}, {} ",
                             savedTaskResource.getNotes(), taskHistoryResource.getNotes());
                    assertEquals(savedTaskResource.getAdditionalProperties(),
                                   taskHistoryResource.getAdditionalProperties(),
                                 "Additional Properties should match");
                    log.info("Additional Properties should match : {}, {} ",
                             savedTaskResource.getAdditionalProperties(),
                             taskHistoryResource.getAdditionalProperties());
                    assertEquals(savedTaskResource.getReconfigureRequestTime(),
                                   taskHistoryResource.getReconfigureRequestTime(),
                                   "Reconfiguration Request Time should match");
                    log.info("Reconfiguration Request Time should match : {}, {} ",
                             savedTaskResource.getReconfigureRequestTime(),
                             taskHistoryResource.getReconfigureRequestTime());
                    assertEquals(savedTaskResource.getLastReconfigurationTime(),
                                   taskHistoryResource.getLastReconfigurationTime(),
                                 "Last Reconfiguration Time should match");
                    log.info("Last Reconfiguration Time should match : {}, {} ",
                             savedTaskResource.getLastReconfigurationTime(),
                             taskHistoryResource.getLastReconfigurationTime());
                    assertEquals(savedTaskResource.getNextHearingId(), taskHistoryResource.getNextHearingId(),
                                 "NextHearingID should match");
                    assertEquals(savedTaskResource.getNextHearingDate(),
                                   taskHistoryResource.getNextHearingDate(),
                                 "Next Hearing Data should Match");
                    assertEquals(savedTaskResource.getPriorityDate().atZoneSameInstant(ZoneId.of("Z")),
                                   taskHistoryResource.getPriorityDate().atZoneSameInstant(ZoneId.of("Z")),
                                 "Get Priority Date should match");

                    return true;
                });

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(savedTaskResource.getTaskId());
                    log.info("Operation {} and reportableTaskList size {}", operation,
                             reportableTaskList.size());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());

                    log.info("Operation {} and reportableTask {}", operation,
                             reportableTaskList.get(0).toString());

                    assertEquals(savedTaskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(savedTaskResource.getDescription(), reportableTaskList.get(0).getDescription());
                    assertEquals(savedTaskResource.getRegionName(), reportableTaskList.get(0).getRegionName());
                    assertEquals(savedTaskResource.getLocationName(), reportableTaskList.get(0).getLocationName());
                    assertEquals(savedTaskResource.getNotes(), reportableTaskList.get(0).getNotes());
                    assertEquals(savedTaskResource.getAdditionalProperties(),
                                 reportableTaskList.get(0).getAdditionalProperties());
                    assertEquals(savedTaskResource.getReconfigureRequestTime(),
                                   reportableTaskList.get(0).getReconfigureRequestTime());
                    assertEquals(savedTaskResource.getLastReconfigurationTime(),
                                   reportableTaskList.get(0).getLastReconfigurationTime());
                    assertEquals(savedTaskResource.getNextHearingId(), reportableTaskList.get(0).getNextHearingId());
                    assertEquals(savedTaskResource.getNextHearingDate(),
                                   reportableTaskList.get(0).getNextHearingDate());
                    assertEquals(savedTaskResource.getPriorityDate().atZoneSameInstant(ZoneId.of("Z")),
                               reportableTaskList.get(0).getPriorityDate().atZoneSameInstant(ZoneId.of("Z")));

                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "LEGAL_OPERATIONS,Legal Operations,PRIVATELAW,Private Law,PRLAPPS,Private Law,ASSIGNED,AutoAssign,Assigned",
        "LEGAL_OPERATIONS,Legal Operations,ST_CIC,Special Tribunals CIC,"
            + "CriminalInjuriesCompensation,Criminal Injuries Compensation,ASSIGNED,AutoAssign,Assigned",
        "CTSC,CTSC,CIVIL,Civil,CIVIL,Civil,UNASSIGNED,Configure,Unassigned",
        "JUDICIAL,Judicial,IA,Immigration and Asylum,Asylum,Asylum,ASSIGNED,AutoAssign,Assigned",
        "ADMIN,Admin,PUBLICLAW,Public Law,PUBLICLAW,Public Law,UNASSIGNED,Configure,Unassigned",
        ",,WA,WA,WaCaseType,WaCaseType,ASSIGNED,AutoAssign,Assigned",
        "TEST,TEST,TEST,TEST,TEST,TEST,UNASSIGNED,Configure,Unassigned"
    })
    void should_save_task_and_get_transformed_labels_from_reportable_task(
        String taskRoleCategory, String reportableTaskRoleCategoryLabel,
        String taskJurisdiction, String reportableTaskJurisdictionLabel,
        String taskCaseTypeId, String reportableTaskCaseTypeLabel,
        String taskState, String lastUpdatedAction, String reportableTaskStateLabel) {
        TaskResource taskResource = buildTaskResource(4,10);
        taskResource.setRoleCategory(taskRoleCategory);
        taskResource.setJurisdiction(taskJurisdiction);
        taskResource.setCaseTypeId(taskCaseTypeId);
        taskResource.setState(CFTTaskState.valueOf(taskState));
        taskResource.setLastUpdatedAction(lastUpdatedAction);
        TaskResource savedTaskResource = taskResourceRepository.save(taskResource);
        checkHistory(savedTaskResource.getTaskId(), 1);

        await()
            .until(
                () -> {
                    log.info("Found reportable task");

                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(savedTaskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    log.info("Found reportable task:{}",reportableTaskList.get(0));
                    assertEquals(savedTaskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(reportableTaskRoleCategoryLabel, reportableTaskList.get(0).getRoleCategoryLabel());
                    assertEquals(reportableTaskJurisdictionLabel, reportableTaskList.get(0).getJurisdictionLabel());
                    assertEquals(reportableTaskCaseTypeLabel, reportableTaskList.get(0).getCaseTypeLabel());
                    assertEquals(reportableTaskStateLabel, reportableTaskList.get(0).getStateLabel());
                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "COMPLETED, completedUserId, Complete, terminatedUserId, Terminate, completed, completedUserId, Completed",
        "CANCELLED, cancelledUserId, Cancel, terminatedUserId, Terminate, deleted, cancelledUserId, Cancelled",
        "CANCELLED, cancelledUserId, Cancel, terminatedUserId, Terminate, cancelled, cancelledUserId,"
            + "Cancelled",
        "UNCONFIGURED, userId, Unconfigured, terminatedUserId, Terminate, deleted, terminatedUserId,"
            + "Cancelled"
    })
    void should_save_task_and_get_outcome_and_agent_name_from_reportable_task(
        String stateBeforeTermination, String lastUpdatedUserBeforeTermination, String updateActionBeforeTermination,
        String lastUpdatedUser, String finalUpdateAction, String terminationReason,
        String expectedAgentName, String expectedOutcome) {
        String taskId = UUID.randomUUID().toString();
        // Create and save initial task resource with UNASSIGNED state
        TaskResource savedTaskResource = createAndSaveThisTask(taskId, "someTaskName",
                                        UNASSIGNED, "Configure", "someAssignee");
        checkHistory(taskId, 1);

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingService.findByReportingTaskId(savedTaskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    log.info("Found reportable task:{}",reportableTaskList.get(0));
                    assertEquals(savedTaskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(UNASSIGNED.getValue(), reportableTaskList.get(0).getState());
                    return true;
                });
        log.info("Updating task to state: {}", stateBeforeTermination);
        savedTaskResource.setState(CFTTaskState.valueOf(stateBeforeTermination));
        savedTaskResource.setLastUpdatedUser(lastUpdatedUserBeforeTermination);
        savedTaskResource.setLastUpdatedAction(updateActionBeforeTermination);
        savedTaskResource.setLastUpdatedTimestamp(OffsetDateTime.now());

        taskResourceRepository.save(savedTaskResource);
        log.info("Updated task Resource {}", savedTaskResource);

        checkHistory(taskId, 2);
        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingService.findByReportingTaskId(savedTaskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    log.info("Found reportable task:{}",reportableTaskList.get(0));
                    assertEquals(savedTaskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(stateBeforeTermination, reportableTaskList.get(0).getState());
                    return true;
                });

        savedTaskResource.setState(TERMINATED);
        savedTaskResource.setLastUpdatedUser(lastUpdatedUser);
        savedTaskResource.setLastUpdatedAction(finalUpdateAction);
        savedTaskResource.setTerminationReason(terminationReason);
        savedTaskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        taskResourceRepository.save(savedTaskResource);

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingService.findByReportingTaskId(savedTaskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    log.info("Found reportable task:{}",reportableTaskList.get(0));
                    assertEquals(savedTaskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(TERMINATED.getValue(), reportableTaskList.get(0).getState());
                    assertEquals(expectedAgentName, reportableTaskList.get(0).getAgentName());
                    assertEquals(expectedOutcome, reportableTaskList.get(0).getOutcome());
                    return true;
                });


    }

    @ParameterizedTest
    @CsvSource(value = {
        "ASSIGNED,AutoAssign,Assigned",
        "UNASSIGNED,Configure,Unassigned",
        "COMPLETED,AutoAssign,Completed",
        "CANCELLED,Configure,Cancelled",
        "TERMINATED,AutoAssign,Terminated",
        "PENDING_RECONFIGURATION,Configure,Pending Reconfiguration",
        "PENDING_AUTO_ASSIGN,Configure,PENDING_AUTO_ASSIGN"
    })
    void should_save_task_and_get_transformed_state_label_from_reportable_task(
        String taskState, String lastUpdatedAction, String reportableTaskStateLabel) {
        TaskResource taskResource = createAndSaveTask();
        String taskId = taskResource.getTaskId();
        checkHistory(taskId, 1);

        taskResource.setState(CFTTaskState.valueOf(taskState));
        taskResource.setLastUpdatedAction(lastUpdatedAction);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        TaskResource savedTaskResource = taskResourceRepository.save(taskResource);
        checkHistory(taskId, 2);

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(savedTaskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(savedTaskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(reportableTaskStateLabel, reportableTaskList.get(0).getStateLabel());
                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "COMPLETED,EXUI_CASE-EVENT_COMPLETION,Automated Completion",
        "COMPLETED,EXUI_USER_COMPLETION,Manual Completion",
        "CANCELLED,CASE_EVENT_CANCELLATION,Automated Cancellation",
        "CANCELLED,EXUI_USER_CANCELLATION,Manual Cancellation",
        "COMPLETED,,"
    })
    void should_save_task_and_get_transformed_termination_process_label_from_reportable_task(
        String taskState, String terminationProcess, String terminationProcessLabel) {
        TaskResource taskResource = createAndSaveTask();
        taskResource.setState(CFTTaskState.valueOf(taskState));
        taskResource.setTerminationProcess(
            TerminationProcess.fromValue(terminationProcess).orElse(null));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        TaskResource savedTaskResource = taskResourceRepository.save(taskResource);

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(savedTaskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(savedTaskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(terminationProcessLabel, reportableTaskList.get(0).getTerminationProcessLabel());
                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "ASSIGNED,AutoAssign,true,true",
        "UNASSIGNED,Configure,false,true",
        "ASSIGNED,Configure,false,false",
        "COMPLETED,AutoAssign,false,false",
        "CANCELLED,Configure,false,false"
    })
    void should_test_task_assignments_and_reportable_tasks_with_and_without_valid_history(
                        String taskState, String lastUpdatedAction,
                        boolean taskAssignmentExists, boolean reportableTaskExists) {
        String taskId = UUID.randomUUID().toString();
        TaskResource taskResource = createAndSaveThisTask(taskId, "someTaskName",
                                        CFTTaskState.valueOf(taskState), lastUpdatedAction, "someAssignee");
        checkHistory(taskId, 1);

        await()
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByAssignmentsTaskId(taskResource.getTaskId());

                    if (taskAssignmentExists) {
                        assertFalse(taskAssignmentsList.isEmpty());
                        assertEquals(1, taskAssignmentsList.size());
                        assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                        assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                        assertEquals(taskResource.getAssignee(), taskAssignmentsList.get(0).getAssignee());
                        assertNull(taskAssignmentsList.get(0).getAssignmentEnd());
                        assertNull(taskAssignmentsList.get(0).getAssignmentEndReason());
                        assertFalse(replicaIntegrationTestUtils.getContainerReplica()
                                        .getLogs().contains(taskResource.getTaskId()
                                         + " : Task with an incomplete history for assignments check"));
                        return true;
                    } else {
                        assertTrue(taskAssignmentsList.isEmpty());
                        if (! ("UNASSIGNED".equals(taskState) && "Configure".equals(lastUpdatedAction))) {
                            assertTrue(replicaIntegrationTestUtils.getContainerReplica()
                                           .getLogs().contains(taskResource.getTaskId()
                                         + " : Task with an incomplete history for assignments check"));
                        } // This is to cover "UNASSIGNED,Configure,false,true"
                        // where the taskHistory is valid but taskAssignment is not created yet.
                        return true;
                    }
                });

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByReportingTaskId(taskId);

                    if (reportableTaskExists) {
                        assertFalse(reportableTaskList.isEmpty());
                        assertEquals(1, reportableTaskList.size());
                        assertEquals(taskId, reportableTaskList.get(0).getTaskId());
                        assertFalse(replicaIntegrationTestUtils.getContainerReplica().getLogs().contains(taskId
                                                        + " : Task with an incomplete history"));
                        return true;
                    } else {
                        assertTrue(reportableTaskList.isEmpty());
                        assertTrue(replicaIntegrationTestUtils.getContainerReplica().getLogs().contains(taskId
                                                        + " : Task with an incomplete history"));
                        return true;
                    }
                });
    }

    @Test
    void should_save_auto_assigned_task_and_get_task_from_reportable_task() {
        TaskResource taskResource = createAndSaveAndAssignTask();
        checkHistory(taskResource.getTaskId(), 1);

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), reportableTaskList.get(0).getTaskName());
                    assertEquals(taskResource.getTitle(), reportableTaskList.get(0).getTaskTitle());
                    assertEquals(taskResource.getAssignee(), reportableTaskList.get(0).getAssignee());
                    assertTrue(taskResource.getLastUpdatedTimestamp().isEqual(reportableTaskList.get(0).getUpdated()));
                    assertEquals(taskResource.getState().toString(), reportableTaskList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), reportableTaskList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), reportableTaskList.get(0).getUpdateAction());
                    assertEquals(0, reportableTaskList.get(0).getWaitTimeDays());
                    assertEquals("00:00:00", reportableTaskList.get(0).getWaitTime());
                    assertTrue(reportableTaskList.get(0).getFirstAssignedDateTime()
                        .isEqual(reportableTaskList.get(0).getUpdated()));
                    assertNotNull(reportableTaskList.get(0).getFirstAssignedDate());
                    assertEquals(0, reportableTaskList.get(0).getNumberOfReassignments());

                    return true;
                });
    }

    @Test
    void should_save_AutoAssign_task_and_get_task_from_reportable_task() {
        TaskResource taskResource = createAndSaveTask();
        String taskId = taskResource.getTaskId();
        checkHistory(taskId, 1);

        taskResource.setState(ASSIGNED);
        taskResource.setLastUpdatedAction("AutoAssign");
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2022-05-07T20:15:50.345875+01:00"));
        taskResourceRepository.save(taskResource);
        checkHistory(taskId, 2);

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), reportableTaskList.get(0).getTaskName());
                    assertEquals(taskResource.getTitle(), reportableTaskList.get(0).getTaskTitle());
                    assertEquals(taskResource.getAssignee(), reportableTaskList.get(0).getAssignee());
                    assertEquals(taskResource.getState().toString(), reportableTaskList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), reportableTaskList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), reportableTaskList.get(0).getUpdateAction());

                    assertEquals(2, reportableTaskList.get(0).getWaitTimeDays());
                    assertEquals("2 days 00:00:05", reportableTaskList.get(0).getWaitTime());

                    return true;
                });

        taskResource.setLastUpdatedAction("Complete");
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2022-06-09T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByReportingTaskId(taskId);

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskId, reportableTaskList.get(0).getTaskId());
                    assertEquals("No", reportableTaskList.get(0).getIsWithinSla());
                    assertEquals(31, reportableTaskList.get(0).getDueDateToCompletedDiffDays());
                    assertEquals(35, reportableTaskList.get(0).getProcessingTimeDays());
                    return true;
                });
    }

    @Test
    void should_save_task_and_get_task_from_task_assignments() {
        TaskResource taskResource = createAndAssignTask("someNewCaseId", "someJurisdiction",
                                                        "someLocation", "someRoleCategory",
                                                        "someTaskName");
        checkHistory(taskResource.getTaskId(), 1);

        await()
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByAssignmentsTaskId(taskResource.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(1, taskAssignmentsList.size());
                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                    assertEquals(taskResource.getAssignee(), taskAssignmentsList.get(0).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(0).getService());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEnd());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEndReason());

                    return true;
                });
    }

    @Test
    void should_save_task_and_get_task_from_task_assignments_with_location_null() {
        TaskResource taskResource = createAndAssignTask("someNewCaseId", "someJurisdiction",
            null, "someRoleCategory", "someTaskName");
        checkHistory(taskResource.getTaskId(), 1);

        await()
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByAssignmentsTaskId(taskResource.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(1, taskAssignmentsList.size());
                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                    assertEquals(taskResource.getAssignee(), taskAssignmentsList.get(0).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(0).getService());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEnd());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEndReason());
                    assertNull(taskResource.getLocation());
                    return true;
                });
    }

    @Test
    void should_save_task_and_get_task_from_task_assignments_with_taskName_null() {
        TaskResource taskResource = createAndAssignTask("someNewCaseId", "someJurisdiction",
                                                        "someLocation", "someRoleCategory", null);
        checkHistory(taskResource.getTaskId(), 1);

        await()
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByAssignmentsTaskId(taskResource.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(1, taskAssignmentsList.size());
                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getAssignee(), taskAssignmentsList.get(0).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(0).getService());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEnd());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEndReason());
                    assertNull(taskResource.getTaskName());
                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "COMPLETED,Complete,COMPLETED, COMPLETED",
        "ASSIGNED,AutoUnassignAssign,REASSIGNED, null",
        "ASSIGNED,UnassignAssign,REASSIGNED, null",
        "ASSIGNED,UnassignClaim,REASSIGNED, null",
        "UNASSIGNED,UnclaimAssign,REASSIGNED, null",
        "UNASSIGNED,Unassign,UNASSIGNED, null",
        "UNASSIGNED,AutoUnassign,UNASSIGNED, null",
        "UNASSIGNED,Unclaim,UNCLAIMED, null",
        "TERMINATED,Terminate,TERMINATED, TERMINATED",
        "ASSIGNED,Cancel,CANCELLED, USER_CANCELLED",
    })
    void should_save_task_and_check_task_assignments(String newState, String lastAction, String endReason,
                                                     String finalState) {
        String taskId = UUID.randomUUID().toString();
        TaskResource taskResource = createAndSaveThisTask(taskId, "someTaskName",
                                                          UNASSIGNED, "Configure");

        taskResource.setAssignee("someAssignee");
        taskResource.setLastUpdatedAction("AutoAssign");
        taskResource.setState(ASSIGNED);
        taskResourceRepository.save(taskResource);

        await()
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByAssignmentsTaskId(taskId);

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(1, taskAssignmentsList.size());
                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                    assertEquals(taskResource.getAssignee(), taskAssignmentsList.get(0).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(0).getService());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEnd());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEndReason());

                    return true;
                });

        if (lastAction.matches("Unassign|Unclaim|AutoUnassign")) {
            taskResource.setAssignee(null);
        }
        if (lastAction.matches("AutoUnassignAssign|UnassignAssign|UnassignClaim|UnclaimAssign|Assign")) {
            taskResource.setAssignee("newAssignee");
        }
        //"2023-03-29T20:15:45.345875+01:00"
        if (lastAction.equals("Complete")) {
            taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-04-07T20:15:55.345875+01:00"));
        } else {
            taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-04-07T20:15:45.345875+01:00"));
        }
        taskResource.setLastUpdatedAction(lastAction);
        taskResource.setState(CFTTaskState.valueOf(newState));
        taskResourceRepository.save(taskResource);

        checkHistory(taskId, 3);

        await()
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByAssignmentsTaskId(taskResource.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    if (lastAction.matches("AutoUnassignAssign|UnassignAssign|UnassignClaim|UnclaimAssign|Assign")) {
                        assertEquals(2, taskAssignmentsList.size());
                    } else {
                        assertEquals(1, taskAssignmentsList.size());
                    }

                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                    assertNotNull(taskAssignmentsList.get(0).getAssignmentEnd());
                    assertEquals(endReason, taskAssignmentsList.get(0).getAssignmentEndReason());

                    return true;
                });

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByReportingTaskId(taskId);

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskId, reportableTaskList.get(0).getTaskId());
                    if (lastAction.matches("Complete|AutoCancel|Cancel")) {
                        assertEquals(finalState, reportableTaskList.get(0).getFinalStateLabel());
                        if (lastAction.equals("Complete")) {

                            assertEquals("No", reportableTaskList.get(0).getIsWithinSla());
                            assertEquals(2, reportableTaskList.get(0).getDueDateToCompletedDiffDays());
                            assertEquals(15, reportableTaskList.get(0).getProcessingTimeDays());
                        }
                    } else {
                        assertNull(reportableTaskList.get(0).getFinalStateLabel());
                        assertNull(reportableTaskList.get(0).getIsWithinSla());
                    }
                    assertNotNull(reportableTaskList.get(0).getLastUpdatedDate());

                    List<TaskHistoryResource> taskHistoryList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByTaskId(taskId);
                    assertEquals(3, taskHistoryList.size());
                    return true;
                });

        if (lastAction.equals("Complete")) {
            await()
                .until(
                    () -> {
                        List<ReportableTaskResource> reportableTaskList
                            = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByReportingTaskId(taskId);

                        assertEquals("9 days 00:00:10", reportableTaskList.get(0).getHandlingTime());
                        assertTrue(reportableTaskList.get(0).getProcessingTime().startsWith("15 days"));
                        assertEquals("-2 days -00:00:10", reportableTaskList.get(0).getDueDateToCompletedDiffTime());
                        return true;
                    });
        }
    }

    @Test
    void should_save_task_and_record_multiple_task_assignments() {
        TaskResource taskResource = createAndAssignTask("someNewCaseId", "someJurisdiction",
                                                        "someLocation", "someRoleCategory",
                                                        "someTaskName");
        checkHistory(taskResource.getTaskId(), 1);
        taskResource.setLastUpdatedAction("Unclaim");
        taskResource.setState(CFTTaskState.UNASSIGNED);
        taskResource.setAssignee(null);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-04-07T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);
        checkHistory(taskResource.getTaskId(), 2);

        taskResource.setLastUpdatedAction("Assign");
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee("NewAssignee");
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-04-12T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);
        checkHistory(taskResource.getTaskId(), 3);

        taskResource.setLastUpdatedAction("Complete");
        taskResource.setState(CFTTaskState.COMPLETED);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-04-17T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);
        checkHistory(taskResource.getTaskId(), 4);

        await()
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByAssignmentsTaskId(taskResource.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(2, taskAssignmentsList.size());

                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                    assertEquals("someAssignee", taskAssignmentsList.get(0).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(0).getService());
                    assertTrue(OffsetDateTime.parse("2023-03-29T20:15:45.345875+01:00").isEqual(
                        taskAssignmentsList.get(0).getAssignmentStart()));
                    assertTrue(OffsetDateTime.parse("2023-04-07T20:15:45.345875+01:00").isEqual(
                        taskAssignmentsList.get(0).getAssignmentEnd()));
                    assertEquals("UNCLAIMED", taskAssignmentsList.get(0).getAssignmentEndReason());

                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(1).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(1).getTaskName());
                    assertEquals("NewAssignee", taskAssignmentsList.get(1).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(1).getService());
                    assertTrue(OffsetDateTime.parse("2023-04-12T20:15:45.345875+01:00").isEqual(
                        taskAssignmentsList.get(1).getAssignmentStart()));
                    assertTrue(OffsetDateTime.parse("2023-04-17T20:15:45.345875+01:00").isEqual(
                        taskAssignmentsList.get(1).getAssignmentEnd()));
                    assertEquals("COMPLETED", taskAssignmentsList.get(1).getAssignmentEndReason());

                    return true;
                });
    }

    @Test
    void given_zero_publications_should_return_false() {

        TaskResourceRepository mocktaskResourceRepository = mock(TaskResourceRepository.class);
        when(mocktaskResourceRepository.countPublications()).thenReturn(0);
        SubscriptionCreator subscriptionCreatorForTest = new
            SubscriptionCreator(TEST_REPLICA_DB_USER, TEST_REPLICA_DB_PASS,
            TEST_PRIMARY_DB_USER, TEST_PRIMARY_DB_PASS, TEST_PUBLICATION_URL, ENVIRONMENT);
        MIReportingService service = new MIReportingService(null, mocktaskResourceRepository,
            reportableTaskRepository,
            taskAssignmentsRepository,
            subscriptionCreatorForTest);
        await()
            .until(() -> {
                assertFalse(service.isPublicationPresent());
                return true;
            });

    }

    @Test
    void given_unknown_task_id_what_happens() {
        List<TaskHistoryResource> taskHistoryResourceList
            = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByTaskId("1111111");
        assertTrue(taskHistoryResourceList.isEmpty());
    }


    @ParameterizedTest
    @CsvSource(value = {
        "UNASSIGNED,Configure",
        "ASSIGNED,AutoAssign",
        "ASSIGNED,Configure",
        "UNASSIGNED,Claim",
        "UNASSIGNED,AutoAssign"
    })
    void should_report_incomplete_task_history(String initialState, String lastAction) {
        String taskId = UUID.randomUUID().toString();
        createAndSaveThisTask(taskId, "someTaskName",
            CFTTaskState.valueOf(initialState), lastAction);

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByReportingTaskId(taskId);

                    if ((initialState.equals("UNASSIGNED") && lastAction.equals("Configure"))
                        || (initialState.equals("ASSIGNED") && lastAction.equals("AutoAssign"))) {
                        assertFalse(reportableTaskList.isEmpty());
                        assertFalse(replicaIntegrationTestUtils.getContainerReplica()
                                        .getLogs().contains(taskId + " : Task with an incomplete history"));
                    } else {
                        assertTrue(reportableTaskList.isEmpty());
                        assertTrue(replicaIntegrationTestUtils.getContainerReplica()
                                       .getLogs().contains(taskId + " : Task with an incomplete history"));
                    }
                    return true;
                });

    }

    private TaskResource buildTaskResource(int daysUntilDue, int daysUntilPriority) {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            UNASSIGNED,
            "987654",
            OffsetDateTime.now().plusDays(daysUntilDue).withHour(10).withMinute(0).withSecond(0).withNano(0)
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(
            OffsetDateTime.now().plusDays(daysUntilPriority).withHour(10).withMinute(0).withSecond(0).withNano(0));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        taskResource.setLastUpdatedAction("Configure");
        return taskResource;
    }

    private void addMissingParameters(TaskResource taskResource, boolean required) {
        taskResource.setDescription(required
            ? "[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)" : null);
        List<NoteResource> notesList = new ArrayList<>();
        final NoteResource noteResource = new NoteResource(
            "someCode",
            "noteTypeVal",
            "userVal",
            "someContent"
        );
        notesList.add(noteResource);
        taskResource.setNotes(required ? notesList : null);
        taskResource.setRegion(required ? "Wales" : null);
        taskResource.setLocationName(required ? "Cardiff" : null);
        taskResource.setAdditionalProperties(required ? Map.of(
            "key1", "value1",
            "key2", "value2",
            "key3", "value3",
            "key4", "value4"
        ) : null);
        taskResource.setReconfigureRequestTime(required
            ? OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0) : null);
        taskResource.setLastReconfigurationTime(required
            ? OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0) : null);
        taskResource.setNextHearingId(required ? "W-CA-1234" : null);
        taskResource.setNextHearingDate(required
            ? OffsetDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0) :  null);
    }

    private TaskResource createAndSaveTask(String caseId,
                                           String taskId,
                                           String taskState,
                                           OffsetDateTime created,
                                           String caseTypeId,
                                           String jurisdiction) {

        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNASSIGNED,
            caseId,
            OffsetDateTime.now().plusDays(4).withHour(10).withMinute(0).withSecond(0).withNano(0)
        );
        taskResource.setCreated(created);
        taskResource.setPriorityDate(OffsetDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0)
                                         .withNano(0));
        taskResource.setRoleCategory("Supervisor");
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setLocation("1111111");
        taskResource.setCaseTypeId(caseTypeId);
        taskResource.setDueDateTime(OffsetDateTime.now().plusDays(2L));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        taskResource.setLastUpdatedAction("Configure");
        taskResource = taskResourceRepository.save(taskResource);

        if (!"UNASSIGNED".equals(taskState)) {
            taskResource.setState(ASSIGNED);
            taskResource.setAssignee("SomeAssignee");
            taskResource.setLastUpdatedAction("AutoAssign");
            taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
            taskResource = taskResourceRepository.save(taskResource);

            if (!"ASSIGNED".equals(taskState)) {
                taskResource.setState(CFTTaskState.valueOf(taskState));
                taskResource.setAssignee("SomeAssignee");
                taskResource.setLastUpdatedAction("AutoAssign");
                taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
                addMissingParameters(taskResource, true);
                if ("TERMINATED".equals(taskState)) {
                    taskResource.setTerminationReason("completed");
                }

                taskResource = taskResourceRepository.save(taskResource);
            }
        }

        return taskResource;
    }

    private TaskResource createAndSaveTask() {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            UNASSIGNED,
            "987654",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-15T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction("Configure");
        return taskResourceRepository.save(taskResource);
    }

    private TaskResource createAndSaveAndAssignTask() {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            ASSIGNED,
            "987654",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-15T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction("AutoAssign");
        return taskResourceRepository.save(taskResource);
    }

    private TaskResource createAndSaveThisTask(String taskId, String taskName,
                                               CFTTaskState taskState, String lastAction) {
        TaskResource taskResource = new TaskResource(
            taskId,
            taskName,
            "someTaskType",
            taskState,
            "987654",
            OffsetDateTime.parse("2023-04-05T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.parse("2023-03-23T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2023-03-26T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction(lastAction);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-03-29T20:15:45.345875+01:00"));
        taskResource.setJurisdiction("someJurisdiction");
        taskResource.setLocation("someLocation");
        taskResource.setRoleCategory("someRoleCategory");
        return taskResourceRepository.save(taskResource);
    }

    private TaskResource createAndSaveThisTask(String taskId, String taskName,
                                               CFTTaskState taskState, String lastAction, String assignee) {
        TaskResource taskResource = new TaskResource(
            taskId,
            taskName,
            "someTaskType",
            taskState,
            "987654",
            OffsetDateTime.parse("2023-04-05T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.parse("2023-03-23T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2023-03-26T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction(lastAction);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-03-29T20:15:45.345875+01:00"));
        taskResource.setJurisdiction("someJurisdiction");
        taskResource.setLocation("someLocation");
        taskResource.setRoleCategory("someRoleCategory");
        taskResource.setAssignee(assignee);
        return taskResourceRepository.save(taskResource);
    }

    private TaskResource createAndAssignTask(String caseId, String jurisdiction, String location, String roleCategory,
                                             String taskName) {

        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            caseId,
            jurisdiction,
            location,
            roleCategory,
            taskName);

        taskResource.setDueDateTime(OffsetDateTime.parse("2023-04-05T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-03-29T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2023-03-26T20:15:45.345875+01:00"));
        taskResource.setCreated(OffsetDateTime.parse("2023-03-23T20:15:45.345875+01:00"));
        taskResource.setAssignee("someAssignee");
        taskResource.setLastUpdatedAction("AutoAssign");
        taskResource.setState(ASSIGNED);
        return taskResourceRepository.save(taskResource);
    }

    private void createAndSaveTaskWithLastReconfigurationTime(String taskId, String taskName,
                                                              CFTTaskState taskState, String lastAction) {
        TaskResource taskResource = new TaskResource(
            taskId,
            taskName,
            "someTaskType",
            taskState,
            "987654",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction(lastAction);
        taskResource.setLastReconfigurationTime(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "UNASSIGNED,Configure",
        "ASSIGNED,AutoAssign",
        "ASSIGNED,Configure"
    })
    void should_insert_first_task_and_update_reconfiguration_task(String state, String lastAction) {

        TaskResource taskResource = createAndSaveTask();
        String taskId = taskResource.getTaskId();
        createAndSaveTaskWithLastReconfigurationTime(taskId,
            "SecondTask", CFTTaskState.valueOf(state), lastAction);

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByReportingTaskId(taskId);

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskId, reportableTaskList.get(0).getTaskId());
                    assertEquals("SecondTask", reportableTaskList.get(0).getTaskName());

                    List<TaskHistoryResource> taskHistoryList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByTaskId(taskId);
                    assertEquals(2, taskHistoryList.size());
                    return true;
                });
    }

    @Test
    public void should_test_mark_functionality_with_refresh() {

        TaskResource taskResource = createAndAssignTask("someNewCaseId", "someJurisdiction",
                                                        "someLocation", "someRoleCategory",
                                                        "someTaskName");

        List<OffsetDateTime> origTaskAssignmentReportRefreshTimes = new ArrayList<>();
        await()
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByAssignmentsTaskId(taskResource.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(1, taskAssignmentsList.size());
                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                    assertEquals(taskResource.getAssignee(), taskAssignmentsList.get(0).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(0).getService());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEnd());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEndReason());
                    origTaskAssignmentReportRefreshTimes.add(taskAssignmentsList.get(0).getReportRefreshTime());

                    return true;
                });

        List<OffsetDateTime> origReportableTaskReportRefreshTimes = new ArrayList<>();
        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(taskResource.getState().getValue(), reportableTaskList.get(0).getState());
                    origReportableTaskReportRefreshTimes.add(reportableTaskList.get(0).getReportRefreshTime());

                    return true;
                });

        MIReplicaDBDao miReplicaDBDao = new MIReplicaDBDao(replicaIntegrationTestUtils.getContainerReplica()
                                                               .getJdbcUrl(),
                                                           replicaIntegrationTestUtils.getContainerReplica()
                                                               .getUsername(),
                                                           replicaIntegrationTestUtils.getContainerReplica()
                                                               .getPassword());
        // Mark filters to select both the tasks
        miReplicaDBDao.callMarkReportTasksForRefresh(null, null, null,
                                      null, null, OffsetDateTime.now(), taskResource.getCreated().minusDays(2L));

        Optional<ReplicaTaskResource> optionalReplicaTaskResource
            = replicaTaskResourceRepository.getByTaskId(taskResource.getTaskId());
        Assertions.assertTrue(optionalReplicaTaskResource.isPresent());
        OffsetDateTime taskRequestRefreshTime = optionalReplicaTaskResource.get().getReportRefreshRequestTime();

        await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(taskResource.getState().getValue(), reportableTaskList.get(0).getState());
                    assertTrue(origReportableTaskReportRefreshTimes.get(0)
                                   .isBefore(reportableTaskList.get(0).getReportRefreshTime()));
                    LocalDateTime reportRefreshTime =
                            reportableTaskList.get(0).getReportRefreshTime().toLocalDateTime();
                    assertThat(reportRefreshTime).isCloseTo(taskRequestRefreshTime.toLocalDateTime(),
                                                           within(100, ChronoUnit.MILLIS));

                    List<TaskAssignmentsResource> taskAssignmentsList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByAssignmentsTaskId(taskResource.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(1, taskAssignmentsList.size());
                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertTrue(origTaskAssignmentReportRefreshTimes.get(0)
                                   .isBefore(taskAssignmentsList.get(0).getReportRefreshTime()));
                    reportRefreshTime =
                        taskAssignmentsList.get(0).getReportRefreshTime().toLocalDateTime();
                    assertThat(reportRefreshTime).isCloseTo(taskRequestRefreshTime.toLocalDateTime(),
                                                           within(100, ChronoUnit.MILLIS));

                    return true;
                });
    }

    @ParameterizedTest
    @MethodSource("markTasksForRefreshArgsProvider")
    public void should_test_procedure_call_mark_report_tasks_for_refresh(final String testCategory,
                                       final String testName,
                                       final Stream<String> taskParamsStream,
                                       final OffsetDateTime markBeforeTime,
                                       final OffsetDateTime markAfterTime,
                                       final Long expectedMarkedCount,
                                       final Stream<String> expectedMarkedTasks) {
        List<TaskResource> tasks = new ArrayList<>();
        taskParamsStream.forEach(taskParamsString -> {
            String[] taskParams = taskParamsString.split(",");
            TaskResource taskResource = createAndSaveTask(
                taskParams[0],
                taskParams[1],
                taskParams[2],
                OffsetDateTime.parse(taskParams[3]),
                taskParams[4],
                taskParams[5]
            );
            log.info(taskResource.toString());
            tasks.add(taskResource);
        });


        List<OffsetDateTime> origTaskAssignmentReportRefreshTimes = new ArrayList<>();

        tasks.forEach(task -> await()
            .until(
                () -> {

                    if ("ASSIGNED".equals(task.getState().getValue())) {
                        List<TaskAssignmentsResource> taskAssignmentsList
                            = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                            .findByAssignmentsTaskId(task.getTaskId());

                        assertFalse(taskAssignmentsList.isEmpty());
                        assertEquals(1, taskAssignmentsList.size());
                        assertEquals(task.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                        assertEquals(task.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                        assertEquals(task.getAssignee(), taskAssignmentsList.get(0).getAssignee());
                        assertEquals(task.getJurisdiction(), taskAssignmentsList.get(0).getService());
                        assertNull(taskAssignmentsList.get(0).getAssignmentEnd());
                        assertNull(taskAssignmentsList.get(0).getAssignmentEndReason());
                        origTaskAssignmentReportRefreshTimes.add(taskAssignmentsList.get(0).getReportRefreshTime());
                    }

                    return true;
                }));

        List<OffsetDateTime> origReportableTaskReportRefreshTimes = new ArrayList<>();
        tasks.forEach(task -> await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(task.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(task.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(task.getState().getValue(), reportableTaskList.get(0).getState());
                    origReportableTaskReportRefreshTimes.add(reportableTaskList.get(0).getReportRefreshTime());

                    return true;
                }));

        List<String> taskIds = tasks.stream().map(TaskResource::getTaskId).toList();
        List<String> caseIds = tasks.stream().map(TaskResource::getCaseId).toList();
        List<String> taskStates =   tasks.stream().map(x -> x.getState().getValue()).toList();

        MIReplicaDBDao miReplicaDBDao = new MIReplicaDBDao(replicaIntegrationTestUtils.getContainerReplica()
                                                               .getJdbcUrl(),
                                                           replicaIntegrationTestUtils.getContainerReplica()
                                                               .getUsername(),
                                                           replicaIntegrationTestUtils.getContainerReplica()
                                                               .getPassword());

        Sort sort = Sort.by(Sort.Order.asc("caseName"));
        List<ReplicaTaskResource> replicaTaskResources =
            replicaTaskResourceRepository.findAllByTaskIdIn(taskIds, sort);
        List<OffsetDateTime> taskRefreshTimestamps = replicaTaskResources.stream()
            .map(ReplicaTaskResource::getReportRefreshRequestTime)
            .filter(Objects::nonNull)
            .toList();
        assertTrue(taskRefreshTimestamps.isEmpty());

        switch (testCategory) {
            case "ProcTestsWithDefaultNulls" ->
                miReplicaDBDao.callMarkReportTasksForRefresh(null, null, null,
                                              null, null, markBeforeTime, markAfterTime);
            case "ProcTestsWithDefaultEmpty" ->
                miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), "",
                                              "", Collections.emptyList(), markBeforeTime, markAfterTime);
            case "MarkBeforeTimeTests" ->
                miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), null,
                                              null, Collections.emptyList(), markBeforeTime, markAfterTime);
            case "TaskStateTests" -> {
                if ("matchPartialRecordsByMultipleStatuses".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), null,
                                             null, List.of("COMPLETED", "TERMINATED"), markBeforeTime, markAfterTime);
                } else if ("matchNoRecordsByInvalidStatuses".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), null,
                                                  null, List.of("DUMMY", "TEST"), markBeforeTime, markAfterTime);
                } else {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), null,
                                                  null, taskStates, markBeforeTime, markAfterTime);
                }
            }
            case "TaskIdTests" -> {
                if ("matchPartialRecordsByMultipleTaskIds".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), taskIds.subList(0, 2), null,
                                                  null, Collections.emptyList(), markBeforeTime, markAfterTime);
                } else if ("matchNoRecordsByInvalidTaskIds".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(),
                                                  Collections.singletonList("alsdjf-aldsj-dummy"), null,
                                                  null, Collections.emptyList(), markBeforeTime, markAfterTime);
                } else {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), taskIds, null,
                                                  null, Collections.emptyList(), markBeforeTime, markAfterTime);
                }
            }
            case "CaseIdTests" -> {
                if ("matchPartialRecordsByMultipleCaseIds".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(caseIds.subList(0, 2), Collections.emptyList(), null,
                                                  null, Collections.emptyList(), markBeforeTime, markAfterTime);
                } else if ("matchNoRecordsByInvalidCaseIds".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.singletonList("9999999"),
                                                  Collections.emptyList(), null,
                                                  null, Collections.emptyList(), markBeforeTime, markAfterTime);
                } else {
                    miReplicaDBDao.callMarkReportTasksForRefresh(caseIds, Collections.emptyList(), null,
                                                  null, Collections.emptyList(), markBeforeTime, markAfterTime);
                }
            }
            case "MarkByJurisdictionTests" ->
                miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), "WA",
                                              "", Collections.emptyList(), markBeforeTime, markAfterTime);
            case "MarkByCaseTypeIdTests" ->
                miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), null,
                                              "WAAPPS", Collections.emptyList(), markBeforeTime, markAfterTime);
            case "MarkByAllParamsTests" -> {
                if ("matchPartialRecordsByAllParams".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(caseIds, taskIds, "TEST",
                                                  "TESTAPPS", taskStates, markBeforeTime, markAfterTime);
                } else if ("matchNoRecordsByInvalidParams".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(caseIds, taskIds, "WA",
                                  "TESTAPPS", Collections.singletonList("DUMMY"), markBeforeTime, markAfterTime);
                } else {
                    miReplicaDBDao.callMarkReportTasksForRefresh(caseIds, taskIds, "WA",
                                                  "WAAPPS", taskStates, markBeforeTime, markAfterTime);
                }
            }
            default -> miReplicaDBDao.callMarkReportTasksForRefresh(caseIds, taskIds, "WA",
                                                     "WAAPPS", taskStates, markBeforeTime, markAfterTime);
        }
        replicaTaskResources = replicaTaskResourceRepository.findAllByTaskIdIn(taskIds, sort);
        taskRefreshTimestamps = replicaTaskResources.stream()
            .map(ReplicaTaskResource::getReportRefreshRequestTime)
            .filter(Objects::nonNull)
            .toList();
        Long count = taskRefreshTimestamps.stream().map(Objects::nonNull).count();
        Assertions.assertEquals(expectedMarkedCount, count, String.format("%s-%s:", testCategory, testName));

        if (count > 0) {
            OffsetDateTime taskRequestRefreshTime =
                taskRefreshTimestamps.stream().filter(Objects::nonNull).findFirst()
                    .orElse(OffsetDateTime.now().minusYears(1L));

            expectedMarkedTasks.forEach(taskId ->
                await()
                .until(
                    () -> {
                        List<ReportableTaskResource> reportableTaskList
                            = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByReportingTaskId(taskId);

                        assertFalse(reportableTaskList.isEmpty());
                        assertEquals(1, reportableTaskList.size());
                        assertEquals(taskId, reportableTaskList.get(0).getTaskId());
                        assertTrue(origReportableTaskReportRefreshTimes.get(0)
                                       .isBefore(reportableTaskList.get(0).getReportRefreshTime()));
                        LocalDateTime reportRefreshTime =
                            reportableTaskList.get(0).getReportRefreshTime().toLocalDateTime();
                        assertThat(reportRefreshTime).isCloseTo(
                            taskRequestRefreshTime.toLocalDateTime(),
                            within(100, ChronoUnit.MILLIS)
                        );

                        if ("ASSIGNED".equals(reportableTaskList.get(0).getState())) {

                            List<TaskAssignmentsResource> taskAssignmentsList
                                = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                                .findByAssignmentsTaskId(taskId);

                            assertFalse(taskAssignmentsList.isEmpty());
                            assertEquals(1, taskAssignmentsList.size());
                            assertEquals(taskId, taskAssignmentsList.get(0).getTaskId());
                            assertTrue(origTaskAssignmentReportRefreshTimes.get(0)
                                           .isBefore(taskAssignmentsList.get(0).getReportRefreshTime()));
                            reportRefreshTime =
                                taskAssignmentsList.get(0).getReportRefreshTime().toLocalDateTime();
                            assertThat(reportRefreshTime).isCloseTo(
                                taskRequestRefreshTime.toLocalDateTime(),
                                within(100, ChronoUnit.MILLIS)
                            );
                        }

                        return true;
                    }));
        }
    }

    public Stream<Arguments> markTasksForRefreshArgsProvider() {
        String waCaseId = "1690202048809201";
        String terminatedStatus = "TERMINATED";
        String waCaseTypeId = "WAAPPS";
        String waJurisdiction = "WA";

        String testCaseId = "16902020444444";
        String unassignedStatus = "UNASSIGNED";
        String testCaseTypeId = "TESTAPPS";
        String testJurisdiction = "TEST";

        String assignedStatus = "ASSIGNED";
        String completedStatus = "COMPLETED";
        String testCaseId1 = "1690202055555";

        List<UUID> taskIds = Stream.generate(UUID::randomUUID).limit(60).toList();

        return Stream.of(
            Arguments.arguments(
                "ProcTestsWithDefaultNulls",
                "matchAllRecords",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(0).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(25), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(1).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(24), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(23).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(26).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(0).toString(),taskIds.get(1).toString())),

            Arguments.arguments(
                "ProcTestsWithDefaultEmpty",
                "matchAllRecords",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(2).toString(), terminatedStatus,
                              OffsetDateTime.now().minusDays(27), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(3).toString(), terminatedStatus,
                              OffsetDateTime.now().minusDays(28), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(26).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(29).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(2).toString(),taskIds.get(3).toString())),

            // The 4'th and 5th Arguments markBeforeTime, markAfterTime are used as filtering criteria
            Arguments.arguments(
                "MarkBeforeTimeTests",
                "matchAllRecords",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(4).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(20), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(5).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(17).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(21).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(4).toString(),taskIds.get(5).toString())),

            // The 4'th and 5th Arguments markBeforeTime, markAfterTime are used as filtering criteria
            Arguments.arguments(
                "MarkBeforeTimeTests",
                "matchPartialRecords",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(6).toString(), terminatedStatus,
                              OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(7).toString(), terminatedStatus,
                              OffsetDateTime.now().minusDays(13), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(14).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                1L, Stream.of(taskIds.get(6).toString())),

            // The 4'th Argument markBeforeTime is used as filtering criteria
            Arguments.arguments(
                "MarkBeforeTimeTests",
                "noneMatchedRecords",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(8).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(12), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(9).toString(), unassignedStatus,
                                  OffsetDateTime.now().minusDays(11), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(21).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(23).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L, Stream.empty()),

            Arguments.arguments(
                "TaskStateTests",
                "matchAllRecordsBySingleStatus",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(10).toString(), completedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(11).toString(), completedStatus,
                                  OffsetDateTime.now().minusDays(17), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(15).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(19).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(10).toString(), taskIds.get(11).toString())),

            Arguments.arguments(
                "TaskStateTests",
                "matchAllRecordsByMultipleStatuses",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(12).toString(), completedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(13).toString(), assignedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(12).toString(), taskIds.get(13).toString())),

            Arguments.arguments(
                "TaskStateTests",
                "matchPartialRecordsByMultipleStatuses",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(14).toString(), completedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(15).toString(), assignedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(16).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(14).toString(), taskIds.get(16).toString())),

            Arguments.arguments(
                "TaskStateTests",
                "matchNoRecordsByInvalidStatuses",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(17).toString(), completedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(18).toString(), assignedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(19).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L, Stream.empty()),

            Arguments.arguments(
                "TaskIdTests",
                "matchAllRecordsByMultipleTaskIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(20).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(21).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(20).toString(), taskIds.get(21).toString())),

            Arguments.arguments(
                "TaskIdTests",
                "matchPartialRecordsByMultipleTaskIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(22).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(23).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(24).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(22).toString(), taskIds.get(23).toString())),

            Arguments.arguments(
                "TaskIdTests",
                "matchNoRecordsByInvalidTaskIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(25).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(26).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(27).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L, Stream.empty()),

            Arguments.arguments(
                "CaseIdTests",
                "matchAllRecordsByMultipleCaseIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(28).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(29).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(28).toString(), taskIds.get(29).toString())),

            Arguments.arguments(
                "CaseIdTests",
                "matchPartialRecordsByMultipleCaseIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(30).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(31).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId1, taskIds.get(32).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(30).toString(), taskIds.get(31).toString())),

            Arguments.arguments(
                "CaseIdTests",
                "matchNoRecordsByInvalidCaseIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(33).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(34).toString(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L, Stream.empty()),

            Arguments.arguments(
                "MarkByJurisdictionTests",
                "matchNoRecordsByInvalidJurisdiction",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(35), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(36), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L, Stream.empty()),

            Arguments.arguments(
                "MarkByJurisdictionTests",
                "matchOneRecordByJurisdiction",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(37), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(38), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                1L, Stream.of(taskIds.get(37).toString())),

            Arguments.arguments(
                "MarkByJurisdictionTests",
                "matchMultipleRecordsByJurisdiction",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(39), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(40), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(39).toString(), taskIds.get(40).toString())),

            Arguments.arguments(
                "MarkByCaseTypeIdTests",
                "matchNoRecordsByInvalidCaseTypeId",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(41), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), testCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(42), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L, Stream.empty()),

            Arguments.arguments(
                "MarkByCaseTypeIdTests",
                "matchOneRecordByCaseTypeId",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(43), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(44), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                1L, Stream.of(taskIds.get(43).toString())),

            Arguments.arguments(
                "MarkByCaseTypeIdTests",
                "matchMultipleRecordsByCaseTypeId",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(45), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(46), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(45).toString(), taskIds.get(46).toString())),

            Arguments.arguments(
                "MarkByAllParamsTests",
                "matchAllRecordsByAllParams",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(47), completedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(48), terminatedStatus,
                                  OffsetDateTime.now().minusDays(17), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(49), assignedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId1, taskIds.get(50), unassignedStatus,
                                  OffsetDateTime.now().minusDays(17), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(15).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(19).withHour(10).withMinute(0).withSecond(0).withNano(0),
                4L, Stream.of(taskIds.get(47).toString(), taskIds.get(48).toString(), taskIds.get(49).toString(),
                              taskIds.get(50).toString())),

            Arguments.arguments(
                "MarkByAllParamsTests",
                "matchPartialRecordsByAllParams",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(51), completedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(52), terminatedStatus,
                                  OffsetDateTime.now().minusDays(17), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(53), assignedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId1, taskIds.get(54), unassignedStatus,
                                  OffsetDateTime.now().minusDays(17), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(19).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L, Stream.of(taskIds.get(52).toString(), taskIds.get(54).toString())),

            Arguments.arguments(
                "MarkByAllParamsTests",
                "matchNoRecordsByInvalidParams",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(55), completedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, taskIds.get(56), terminatedStatus,
                                  OffsetDateTime.now().minusDays(17), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, taskIds.get(57), assignedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId1, taskIds.get(58), unassignedStatus,
                                  OffsetDateTime.now().minusDays(17), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                OffsetDateTime.now().minusDays(19).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L, Stream.empty())
        );
    }


    private void checkHistory(String id, int records) {
        await()
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByTaskId(id);

                    assertFalse(taskHistoryResourceList.isEmpty());
                    assertEquals(records, taskHistoryResourceList.size());
                    log.info("task with ID found on MI Reporting task History is :{}",
                             taskHistoryResourceList.isEmpty());
                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "2, 10000, 2",
        "2, -20, 2",
        "2, 0, 2"
    })
    public void should_test_refresh_report_tasks(Integer taskResourcesToCreate,
                                                 Integer maxRowsToProcess,
                                                 Integer expectedProcessed) {
        List<TaskResource> tasks = new ArrayList<>();
        IntStream.range(0, taskResourcesToCreate).forEach(x -> {
            TaskResource taskResource = createAndAssignTask("someNewCaseId", "someJurisdiction",
                                                            "someLocation", "someRoleCategory",
                                                            "someTaskName");
            log.info(taskResource.toString());
            tasks.add(taskResource);
        });

        tasks.forEach(task -> await()
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByReportingTaskId(task.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    ReportableTaskResource reportableTaskResource = reportableTaskList.get(0);
                    assertEquals(task.getTaskId(), reportableTaskResource.getTaskId());
                    assertEquals(task.getState().getValue(), reportableTaskResource.getState());

                    List<TaskAssignmentsResource> taskAssignmentsList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                            .findByAssignmentsTaskId(task.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(1, taskAssignmentsList.size());

                    return true;
                }));

        List<String> taskIds = tasks.stream().map(TaskResource::getTaskId).toList();
        MIReplicaDBDao miReplicaDBDao = new MIReplicaDBDao(replicaIntegrationTestUtils.getContainerReplica()
                                                               .getJdbcUrl(),
                                                             replicaIntegrationTestUtils.getContainerReplica()
                                                                 .getUsername(),
                                                             replicaIntegrationTestUtils.getContainerReplica()
                                                               .getPassword());

        Sort sort = Sort.by(Sort.Order.asc("caseName"));
        List<ReplicaTaskResource> replicaTaskResources =
            replicaTaskResourceRepository.findAllByTaskIdIn(taskIds, sort);
        List<OffsetDateTime> taskRefreshTimestamps = replicaTaskResources.stream()
            .map(ReplicaTaskResource::getReportRefreshRequestTime)
            .filter(Objects::nonNull)
            .toList();
        assertTrue(taskRefreshTimestamps.isEmpty());

        miReplicaDBDao.callMarkReportTasksForRefresh(null, taskIds, null,
                                      null, null, OffsetDateTime.now(),
                                                     tasks.get(0).getCreated().minusDays(5));

        replicaTaskResources = replicaTaskResourceRepository.findAllByTaskIdIn(taskIds, sort);
        taskRefreshTimestamps = replicaTaskResources.stream()
            .map(ReplicaTaskResource::getReportRefreshRequestTime)
            .filter(Objects::nonNull)
            .toList();
        long count = taskRefreshTimestamps.stream().map(Objects::nonNull).count();
        Assertions.assertEquals(taskResourcesToCreate,
                                (int) count, String.format("Should mark all %s tasks:", taskResourcesToCreate));

        miReplicaDBDao.callRefreshReportTasks(maxRowsToProcess);

        await()
            .until(
                () -> {
                    List<ReplicaTaskResource> replicaTaskResourceList =
                        replicaTaskResourceRepository.findAllByTaskIdIn(taskIds, sort);
                    List<OffsetDateTime> taskRefreshTimestampList = replicaTaskResourceList.stream()
                        .map(ReplicaTaskResource::getReportRefreshRequestTime)
                        .filter(Objects::nonNull)
                        .toList();

                    long countNotRefreshed = taskRefreshTimestampList.stream().map(Objects::nonNull).count();
                    Assertions.assertEquals(expectedProcessed, taskResourcesToCreate - (int) countNotRefreshed,
                                            String.format("Should refresh %s tasks:", expectedProcessed));
                    return true;
                });

        AtomicInteger reportableTasksRefreshedCount = new AtomicInteger();
        AtomicInteger taskAssignmentsRefreshedCount = new AtomicInteger();

        taskIds.forEach(x -> {
            List<ReportableTaskResource> reportableTaskList
                = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByReportingTaskId(x);

            assertFalse(reportableTaskList.isEmpty());
            assertEquals(1, reportableTaskList.size());
            assertEquals(x, reportableTaskList.get(0).getTaskId());
            if (reportableTaskList.get(0).getReportRefreshTime() != null) {
                reportableTasksRefreshedCount.getAndIncrement();
            }

            List<TaskAssignmentsResource> taskAssignmentsList
                = replicaIntegrationTestUtils.getMiReportingServiceForTest().findByAssignmentsTaskId(x);

            assertFalse(taskAssignmentsList.isEmpty());
            assertEquals(1, taskAssignmentsList.size());
            assertEquals(x, taskAssignmentsList.get(0).getTaskId());
            if (taskAssignmentsList.get(0).getReportRefreshTime() != null) {
                taskAssignmentsRefreshedCount.getAndIncrement();
            }
        });

        assertEquals(expectedProcessed, reportableTasksRefreshedCount.get());
        assertEquals(expectedProcessed, taskAssignmentsRefreshedCount.get());
    }

}
