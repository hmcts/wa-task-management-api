package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
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

    @Test
    void given_unknown_task_id_get_empty_list() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.getByTaskId("1111111")).thenReturn(Collections.emptyList());
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
                                                    null);

        List<TaskHistoryResource> taskHistoryResourceList
            = miReportingService.findByTaskId("1111111");
        assertTrue(taskHistoryResourceList.isEmpty());
    }

    @Test
    void given_zero_publications_should_return_false() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countPublications()).thenReturn(0);
        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    null);

        assertFalse(miReportingService.isPublicationPresent());
    }

    @Test
    void given_one_publication_should_return_true() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countPublications()).thenReturn(1);
        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    null);

        assertTrue(miReportingService.isPublicationPresent());
    }

    @Test
    void given_zero_replication_slots_should_return_false() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countReplicationSlots()).thenReturn(0);
        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    null);

        assertFalse(miReportingService.isReplicationSlotPresent());
    }

    @Test
    void given_one_replication_slot_should_return_true() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countReplicationSlots()).thenReturn(1);
        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    null);

        assertTrue(miReportingService.isReplicationSlotPresent());
    }

    @Test
    void given_zero_subscriptions_should_return_false() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(0);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
                                                    null);

        assertFalse(miReportingService.isSubscriptionPresent());
    }

    @Test
    void given_one_subscription_should_return_true() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(1);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
                                                    null);

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
                                                    subscriptionCreator);
        miReportingService.logicalReplicationCheck();

        verify(taskResourceRepository, times(1)).createReplicationSlot();
    }



}
