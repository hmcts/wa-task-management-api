package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.List;

@Service
@Profile("replica | preview")
public class MIReportingService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MIReportingService.class);
    public static final String MAIN_SLOT_NAME = "main_slot_v1";

    private static final int PUBLICATION_ROW_COUNT_WITHOUT_WORK_TYPES = 1;

    private final TaskHistoryResourceRepository taskHistoryRepository;
    private final TaskResourceRepository taskResourceRepository;
    private final ReportableTaskRepository reportableTaskRepository;

    @Autowired
    private final SubscriptionCreator subscriptionCreator;

    public MIReportingService(TaskHistoryResourceRepository tasksHistoryRepository,
                              TaskResourceRepository taskResourceRepository,
                              ReportableTaskRepository reportableTaskRepository,
                              SubscriptionCreator subscriptionCreator) {
        this.taskHistoryRepository = tasksHistoryRepository;
        this.taskResourceRepository = taskResourceRepository;
        this.reportableTaskRepository = reportableTaskRepository;
        this.subscriptionCreator = subscriptionCreator;
    }

    public List<TaskHistoryResource> findByTaskId(String taskId) {
        return taskHistoryRepository.findAllByTaskIdOrderByUpdatedAsc(taskId);
    }

    public List<ReportableTaskResource> findByReportingTaskId(String taskId) {
        return reportableTaskRepository.findAllByTaskIdOrderByUpdatedAsc(taskId);
    }

    public void logicalReplicationCheck() {
        LOGGER.debug("Postgresql logical replication check executed");
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
            LOGGER.info("Creating logical replication slot");
            createReplicationSlot();
        }
    }

    protected boolean isReplicationSlotPresent() {
        int count = taskResourceRepository.countReplicationSlots();
        if (count == 0) {
            LOGGER.info("No logical replication slot present for " + MAIN_SLOT_NAME);
            return false;
        } else {
            return true;
        }
    }

    private void createReplicationSlot() {
        taskResourceRepository.createReplicationSlot();
        LOGGER.info("Created logical replication slot " + MAIN_SLOT_NAME);
    }

    protected boolean isPublicationPresent() {
        int count = taskResourceRepository.countPublications();
        if (count == 0) {
            LOGGER.info("No publication present");
            return false;
        } else {
            return true;
        }
    }

    protected boolean isWorkTypesInPublication() {
        if (taskResourceRepository.countPublicationTables() == PUBLICATION_ROW_COUNT_WITHOUT_WORK_TYPES) {
            LOGGER.info("Work types not added to publication");
            return false;
        } else {
            return true;
        }
    }

    private void createPublication() {
        taskResourceRepository.createPublication();
        LOGGER.info("Created publication");
    }

    private void addWorkTypesToPublication() {
        taskResourceRepository.addWorkTypesToPublication();
        LOGGER.info("Added work types to publication");
    }

    protected boolean isSubscriptionPresent() {
        int count = taskHistoryRepository.countSubscriptions();
        if (count == 0) {
            LOGGER.info("No subscription present");
            return false;
        } else {
            return true;
        }
    }

}
