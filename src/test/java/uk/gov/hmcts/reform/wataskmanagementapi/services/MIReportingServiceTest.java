package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MIReportingServiceTest {

    @Autowired
    MIReportingService miReportingService;
    @Autowired
    ReportableTaskRepository reportableTaskRepository;
    @Autowired
    TaskAssignmentsRepository taskAssignmentsRepository;

    @Test
    void given_unknown_task_id_get_empty_list() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.findAllByTaskIdOrderByUpdatedAsc("1111111"))
            .thenReturn(Collections.emptyList());
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
                                                    null, null, null);

        List<TaskHistoryResource> taskHistoryResourceList
            = miReportingService.findByTaskId("1111111");
        assertTrue(taskHistoryResourceList.isEmpty());
    }

    @Test
    void given_zero_publications_should_return_false() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countPublications()).thenReturn(0);
        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    null, null, null);

        assertFalse(miReportingService.isPublicationPresent());
    }

    @Test
    void given_one_publication_should_return_true() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countPublications()).thenReturn(1);
        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    null, null, null);

        assertTrue(miReportingService.isPublicationPresent());
    }

    @Test
    void given_no_work_types_in_publication_should_return_false_and_add_them() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countReplicationSlots()).thenReturn(1);
        when(taskResourceRepository.countPublications()).thenReturn(1);
        when(taskResourceRepository.countPublicationTables()).thenReturn(1);

        SubscriptionCreator subscriptionCreator = mock(SubscriptionCreator.class);

        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(1);

        miReportingService = new MIReportingService(taskHistoryResourceRepository, taskResourceRepository,
                                                    null, taskAssignmentsRepository,
                                                    subscriptionCreator);

        assertTrue(miReportingService.isPublicationPresent());
        assertFalse(miReportingService.isWorkTypesInPublication());

        miReportingService.logicalReplicationCheck();

        verify(taskResourceRepository, times(1)).addWorkTypesToPublication();
        verify(subscriptionCreator, times(1)).refreshSubscription();
    }

    @Test
    void given_work_types_in_publication_should_return_true() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countPublications()).thenReturn(1);
        when(taskResourceRepository.countPublicationTables()).thenReturn(2);

        miReportingService = new MIReportingService(null, taskResourceRepository, null,
                                                    null, null);

        assertTrue(miReportingService.isPublicationPresent());
        assertTrue(miReportingService.isWorkTypesInPublication());
    }

    @Test
    void given_zero_replication_slots_should_return_false() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countReplicationSlots()).thenReturn(0);
        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    null, null, null);

        assertFalse(miReportingService.isReplicationSlotPresent());
    }

    @Test
    void given_one_replication_slot_should_return_true() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countReplicationSlots()).thenReturn(1);
        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    null, null, null);

        assertTrue(miReportingService.isReplicationSlotPresent());
    }

    @Test
    void given_zero_subscriptions_should_return_false() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(0);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
                                                    null, null, null);

        assertFalse(miReportingService.isSubscriptionPresent());
    }

    @Test
    void given_one_subscription_should_return_true() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(1);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
                                                    null, null, null);

        assertTrue(miReportingService.isSubscriptionPresent());
    }

    @Test
    void given_one_replication_slot_should_create_Pub_sub() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countReplicationSlots()).thenReturn(1);
        when(taskResourceRepository.countPublications()).thenReturn(0);
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(0);
        SubscriptionCreator subscriptionCreator = mock(SubscriptionCreator.class);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, taskResourceRepository,
                                                    reportableTaskRepository,
                                                    taskAssignmentsRepository,
                                                    subscriptionCreator);
        miReportingService.logicalReplicationCheck();

        verify(taskResourceRepository, times(1)).createPublication();
        verify(subscriptionCreator, times(1)).createSubscription();
    }

    @Test
    void given_zero_replication_slot_should_create_replication() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countReplicationSlots()).thenReturn(0);

        SubscriptionCreator subscriptionCreator = mock(SubscriptionCreator.class);

        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    reportableTaskRepository,
                                                    taskAssignmentsRepository,
                                                    subscriptionCreator);
        miReportingService.logicalReplicationCheck();

        verify(taskResourceRepository, times(1)).createReplicationSlot();
    }

}
