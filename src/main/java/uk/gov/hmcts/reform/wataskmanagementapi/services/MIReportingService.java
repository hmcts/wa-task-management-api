package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskAssignmentsResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@Profile("replica | preview")
public class MIReportingService {
    public static final String MAIN_SLOT_NAME = "main_slot_v1";
    public static final String WAL_LEVEL = "logical";

    private static final int PUBLICATION_ROW_COUNT_WITHOUT_WORK_TYPES = 1;

    private final TaskHistoryResourceRepository taskHistoryRepository;
    private final TaskResourceRepository taskResourceRepository;
    private final ReportableTaskRepository reportableTaskRepository;
    private final TaskAssignmentsRepository taskAssignmentsRepository;

    @Autowired
    private final SubscriptionCreator subscriptionCreator;

    public MIReportingService(TaskHistoryResourceRepository tasksHistoryRepository,
                              TaskResourceRepository taskResourceRepository,
                              ReportableTaskRepository reportableTaskRepository,
                              TaskAssignmentsRepository taskAssignmentsRepository,
                              SubscriptionCreator subscriptionCreator) {
        this.taskHistoryRepository = tasksHistoryRepository;
        this.taskResourceRepository = taskResourceRepository;
        this.reportableTaskRepository = reportableTaskRepository;
        this.taskAssignmentsRepository = taskAssignmentsRepository;
        this.subscriptionCreator = subscriptionCreator;
    }

    public List<TaskHistoryResource> findByTaskId(String taskId) {
        return taskHistoryRepository.findAllByTaskIdOrderByUpdatedAsc(taskId);
    }

    public List<TaskHistoryResource> findByTaskIdOrderByLatestUpdate(String taskId) {
        return taskHistoryRepository.findAllByTaskIdOrderByUpdatedDesc(taskId);
    }

    public List<ReportableTaskResource> findByReportingTaskId(String taskId) {
        return reportableTaskRepository.findAllByTaskIdOrderByUpdatedAsc(taskId);
    }

    public List<TaskAssignmentsResource> findByAssignmentsTaskId(String taskId) {
        return taskAssignmentsRepository.findAllByTaskIdOrderByAssignmentIdAsc(taskId);
    }

    public void logicalReplicationCheck() {
        log.info("Postgresql logical replication check executed . . .");

        Objects.requireNonNull(taskResourceRepository, "Primary Task DB repo is null.");
        Objects.requireNonNull(taskResourceRepository, "Replica Task DB repo is null.");

        if (!taskResourceRepository.showWalLevel().equals(WAL_LEVEL)
            || !reportableTaskRepository.showWalLevel().equals(WAL_LEVEL)) {

            log.error("WAL LEVEL for primary DB; {}, replicaDB: {}.  These must be set to logical",
                taskResourceRepository.showWalLevel(),
                reportableTaskRepository.showWalLevel());
            return;
        }


        if (isReplicationSlotPresent()) {
            if (isPublicationPresent()) {
                if (!isWorkTypesInPublication()) {
                    addWorkTypesToPublication();
                    subscriptionCreator.refreshSubscription();
                }
            } else {
                createPublication();
            }
            if (!isSubscriptionPresent()) {
                subscriptionCreator.createSubscription();
            }
        } else {
            log.info("Creating logical replication slot");
            createReplicationSlot();
        }
    }

    public boolean hasReplicationStarted() {
        return isReplicationSlotPresent() && isPublicationPresent() && isSubscriptionPresent();

    }

    protected boolean isReplicationSlotPresent() {
        int count = taskResourceRepository.countReplicationSlots();
        if (count == 0) {
            log.info("No logical replication slot present for " + MAIN_SLOT_NAME);
            return false;
        } else {
            return true;
        }
    }

    private void createReplicationSlot() {
        taskResourceRepository.createReplicationSlot();
        log.info("Created logical replication slot " + MAIN_SLOT_NAME);
    }

    protected boolean isPublicationPresent() {
        int count = taskResourceRepository.countPublications();
        if (count == 0) {
            log.info("No publication present");
            return false;
        } else {
            return true;
        }
    }

    protected boolean isWorkTypesInPublication() {
        if (taskResourceRepository.countPublicationTables() == PUBLICATION_ROW_COUNT_WITHOUT_WORK_TYPES) {
            log.info("Work types not added to publication");
            return false;
        } else {
            return true;
        }
    }

    private void createPublication() {
        taskResourceRepository.createPublication();
        log.info("Created publication");
    }

    private void addWorkTypesToPublication() {
        taskResourceRepository.addWorkTypesToPublication();
        log.info("Added work types to publication");
    }

    protected boolean isSubscriptionPresent() {
        int count = taskHistoryRepository.countSubscriptions();
        if (count == 0) {
            log.info("No subscription present");
            return false;
        } else {
            return true;
        }
    }

}
