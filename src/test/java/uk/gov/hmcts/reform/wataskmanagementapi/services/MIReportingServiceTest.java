package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskAssignmentsResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MIReportingServiceTest {

    MIReportingService miReportingService;

    TaskAssignmentsRepository taskAssignmentsRepository = mock(TaskAssignmentsRepository.class);

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
    void given_unknown_task_id_get_empty_list_for_descending_order() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.findAllByTaskIdOrderByUpdatedDesc("1111111"))
            .thenReturn(Collections.emptyList());
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
            null, null, null);

        List<TaskHistoryResource> taskHistoryResourceList
            = miReportingService.findByTaskIdOrderByLatestUpdate("1111111");
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
        ReportableTaskRepository reportableTaskMock = mock(ReportableTaskRepository.class);
        when(taskResourceRepository.countReplicationSlots()).thenReturn(1);
        when(taskResourceRepository.countPublications()).thenReturn(1);
        when(taskResourceRepository.countPublicationTables()).thenReturn(1);
        when(taskResourceRepository.showWalLevel()).thenReturn("logical");
        when(reportableTaskMock.showWalLevel()).thenReturn("replica");

        SubscriptionCreator subscriptionCreator = mock(SubscriptionCreator.class);

        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(1);

        miReportingService = new MIReportingService(taskHistoryResourceRepository, taskResourceRepository,
            reportableTaskMock, taskAssignmentsRepository,
            subscriptionCreator);

        assertTrue(miReportingService.isPublicationPresent());
        assertFalse(miReportingService.isWorkTypesInPublication());

        miReportingService.logicalReplicationCheck();

        verify(taskResourceRepository, times(1)).addWorkTypesToPublication();
        verify(subscriptionCreator, times(1)).refreshSubscription();
        verify(taskAssignmentsRepository, never()).findAllByTaskIdOrderByAssignmentIdAsc("123");
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
        ReportableTaskRepository reportableTaskMock = mock(ReportableTaskRepository.class);
        when(taskResourceRepository.countReplicationSlots()).thenReturn(1);
        when(taskResourceRepository.countPublications()).thenReturn(0);
        when(taskResourceRepository.showWalLevel()).thenReturn("logical");
        when(reportableTaskMock.showWalLevel()).thenReturn("replica");
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(0);
        SubscriptionCreator subscriptionCreator = mock(SubscriptionCreator.class);

        miReportingService = new MIReportingService(
            taskHistoryResourceRepository,
            taskResourceRepository,
            reportableTaskMock,
            taskAssignmentsRepository,
            subscriptionCreator);
        miReportingService.logicalReplicationCheck();

        verify(taskResourceRepository, times(1)).createPublication();
        verify(subscriptionCreator, times(1)).createSubscription();
    }

    @Test
    void given_zero_replication_slot_should_create_replication() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        ReportableTaskRepository reportableTaskMock = mock(ReportableTaskRepository.class);
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);

        when(taskResourceRepository.countReplicationSlots()).thenReturn(0);
        when(taskResourceRepository.showWalLevel()).thenReturn("logical");
        when(taskResourceRepository.countPublications()).thenReturn(0);
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(0);
        when(reportableTaskMock.showWalLevel()).thenReturn("replica");

        SubscriptionCreator subscriptionCreator = mock(SubscriptionCreator.class);
        miReportingService = new MIReportingService(taskHistoryResourceRepository,
            taskResourceRepository,
            reportableTaskMock,
            taskAssignmentsRepository,
            subscriptionCreator);

        miReportingService.logicalReplicationCheck();

        verify(taskResourceRepository, times(1)).createReplicationSlot();
        verify(taskResourceRepository, times(1)).createPublication();
        verify(subscriptionCreator, times(1)).createSubscription();
    }

    @Test
    void should_return_if_primary_wal_level_is_not_logical() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        ReportableTaskRepository reportableTaskMock = mock(ReportableTaskRepository.class);

        when(taskResourceRepository.countReplicationSlots()).thenReturn(0);
        when(taskResourceRepository.showWalLevel()).thenReturn("replica");

        miReportingService = new MIReportingService(taskHistoryResourceRepository,
            taskResourceRepository,
            reportableTaskMock,
            taskAssignmentsRepository,
            mock(SubscriptionCreator.class));

        miReportingService.logicalReplicationCheck();

        verify(taskResourceRepository, times(0)).createReplicationSlot();
        verify(taskResourceRepository, times(0)).createPublication();
        verify(taskResourceRepository, times(2)).showWalLevel();

    }

    @Test
    void should_call_reportable_task_repo_and_return_result() {
        ReportableTaskRepository reportableTaskMock = mock(ReportableTaskRepository.class);
        TaskResourceRepository taskResourceRepositoryMock = mock(TaskResourceRepository.class);

        when(reportableTaskMock.findAllByTaskIdOrderByUpdatedAsc(anyString()))
            .thenReturn(newArrayList());

        miReportingService = new MIReportingService(null,
            taskResourceRepositoryMock,
            reportableTaskMock,
            taskAssignmentsRepository,
            mock(SubscriptionCreator.class));

        miReportingService.findByReportingTaskId("123");

        verify(reportableTaskMock, times(1)).findAllByTaskIdOrderByUpdatedAsc("123");

    }

    @Test
    void should_call_task_assignments_repo_and_return_result() {

        ReportableTaskRepository reportableTaskMock = mock(ReportableTaskRepository.class);
        TaskResourceRepository taskResourceRepositoryMock = mock(TaskResourceRepository.class);

        when(taskAssignmentsRepository.findAllByTaskIdOrderByAssignmentIdAsc(anyString()))
            .thenReturn(newArrayList());

        miReportingService = new MIReportingService(
            null,
            taskResourceRepositoryMock,
            reportableTaskMock,
            taskAssignmentsRepository,
            mock(SubscriptionCreator.class)
        );

        miReportingService.findByAssignmentsTaskId("123");

        verify(taskAssignmentsRepository, times(1)).findAllByTaskIdOrderByAssignmentIdAsc("123");


    }

    @Test
    void should_return_true_if_replication_has_started() {
        ReportableTaskRepository reportableTaskMock = mock(ReportableTaskRepository.class);
        TaskResourceRepository taskResourceRepositoryMock = mock(TaskResourceRepository.class);
        TaskHistoryResourceRepository taskHistoryResourceRepositoryMock = mock(TaskHistoryResourceRepository.class);

        miReportingService = new MIReportingService(taskHistoryResourceRepositoryMock,
            taskResourceRepositoryMock,
            reportableTaskMock,
            taskAssignmentsRepository,
            mock(SubscriptionCreator.class));

        when(taskResourceRepositoryMock.countReplicationSlots()).thenReturn(1);
        when(taskResourceRepositoryMock.countPublications()).thenReturn(1);
        when(taskHistoryResourceRepositoryMock.countSubscriptions()).thenReturn(1);

        assertTrue(miReportingService.hasReplicationStarted());

    }

    @ParameterizedTest
    @CsvSource({
        "0, 1, 1, false",  // No slot
        "1, 0, 1, false",  // No publication
        "1, 1, 0, false",  // No subscription
        "1, 1, 1, true"    // All conditions met
    })
    void should_return_expected_replication_status(int replicationSlots, int publications,
                                                   int subscriptions, boolean expectedResult) {
        ReportableTaskRepository reportableTaskMock = mock(ReportableTaskRepository.class);
        TaskResourceRepository taskResourceRepositoryMock = mock(TaskResourceRepository.class);
        TaskHistoryResourceRepository taskHistoryResourceRepositoryMock = mock(TaskHistoryResourceRepository.class);

        miReportingService = new MIReportingService(
            taskHistoryResourceRepositoryMock,
            taskResourceRepositoryMock,
            reportableTaskMock,
            taskAssignmentsRepository,
            mock(SubscriptionCreator.class)
        );

        when(taskResourceRepositoryMock.countReplicationSlots()).thenReturn(replicationSlots);
        when(taskResourceRepositoryMock.countPublications()).thenReturn(publications);
        when(taskHistoryResourceRepositoryMock.countSubscriptions()).thenReturn(subscriptions);

        Assertions.assertEquals(expectedResult, miReportingService.hasReplicationStarted());
    }

    // ============ Null Parameter Tests ============

    @Test
    void given_null_task_id_should_pass_null_to_repository_for_findByTaskId() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.findAllByTaskIdOrderByUpdatedAsc(null))
            .thenReturn(Collections.emptyList());
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
            null, null, null);

        List<TaskHistoryResource> result = miReportingService.findByTaskId(null);

        assertTrue(result.isEmpty());
        verify(taskHistoryResourceRepository, times(1)).findAllByTaskIdOrderByUpdatedAsc(null);
    }

    @Test
    void given_null_task_id_should_pass_null_to_repository_for_findByTaskIdOrderByLatestUpdate() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.findAllByTaskIdOrderByUpdatedDesc(null))
            .thenReturn(Collections.emptyList());
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
            null, null, null);

        List<TaskHistoryResource> result = miReportingService.findByTaskIdOrderByLatestUpdate(null);

        assertTrue(result.isEmpty());
        verify(taskHistoryResourceRepository, times(1)).findAllByTaskIdOrderByUpdatedDesc(null);
    }

    @Test
    void given_null_task_id_should_pass_null_to_repository_for_findByReportingTaskId() {
        ReportableTaskRepository reportableTaskRepository = mock(ReportableTaskRepository.class);
        when(reportableTaskRepository.findAllByTaskIdOrderByUpdatedAsc(null))
            .thenReturn(Collections.emptyList());
        miReportingService = new MIReportingService(null, null,
            reportableTaskRepository, null, null);

        List<ReportableTaskResource> result = miReportingService.findByReportingTaskId(null);

        assertTrue(result.isEmpty());
        verify(reportableTaskRepository, times(1)).findAllByTaskIdOrderByUpdatedAsc(null);
    }

    @Test
    void given_null_task_id_should_pass_null_to_repository_for_findByAssignmentsTaskId() {
        TaskAssignmentsRepository taskAssignmentsRepo = mock(TaskAssignmentsRepository.class);
        when(taskAssignmentsRepo.findAllByTaskIdOrderByAssignmentIdAsc(null))
            .thenReturn(Collections.emptyList());
        miReportingService = new MIReportingService(null, null,
            null, taskAssignmentsRepo, null);

        List<TaskAssignmentsResource> result = miReportingService.findByAssignmentsTaskId(null);

        assertTrue(result.isEmpty());
        verify(taskAssignmentsRepo, times(1)).findAllByTaskIdOrderByAssignmentIdAsc(null);
    }

    // ============ Non-Empty Results Tests ============

    @Test
    void given_valid_task_id_should_return_task_history_list() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        TaskHistoryResource historyResource1 = mock(TaskHistoryResource.class);
        TaskHistoryResource historyResource2 = mock(TaskHistoryResource.class);
        List<TaskHistoryResource> expectedList = newArrayList(historyResource1, historyResource2);

        when(taskHistoryResourceRepository.findAllByTaskIdOrderByUpdatedAsc("task-123"))
            .thenReturn(expectedList);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
            null, null, null);

        List<TaskHistoryResource> result = miReportingService.findByTaskId("task-123");

        assertEquals(2, result.size());
        assertEquals(expectedList, result);
    }

    @Test
    void given_valid_task_id_should_return_task_history_in_descending_order() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        TaskHistoryResource historyResource1 = mock(TaskHistoryResource.class);
        TaskHistoryResource historyResource2 = mock(TaskHistoryResource.class);
        List<TaskHistoryResource> expectedList = newArrayList(historyResource1, historyResource2);

        when(taskHistoryResourceRepository.findAllByTaskIdOrderByUpdatedDesc("task-123"))
            .thenReturn(expectedList);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
            null, null, null);

        List<TaskHistoryResource> result = miReportingService.findByTaskIdOrderByLatestUpdate("task-123");

        assertEquals(2, result.size());
        assertEquals(expectedList, result);
    }

    @Test
    void given_valid_task_id_should_return_reportable_tasks() {
        ReportableTaskRepository reportableTaskRepository = mock(ReportableTaskRepository.class);
        ReportableTaskResource reportableTask1 = mock(ReportableTaskResource.class);
        ReportableTaskResource reportableTask2 = mock(ReportableTaskResource.class);
        List<ReportableTaskResource> expectedList = newArrayList(reportableTask1, reportableTask2);

        when(reportableTaskRepository.findAllByTaskIdOrderByUpdatedAsc("task-123"))
            .thenReturn(expectedList);
        miReportingService = new MIReportingService(null, null,
            reportableTaskRepository, null, null);

        List<ReportableTaskResource> result = miReportingService.findByReportingTaskId("task-123");

        assertEquals(2, result.size());
        assertEquals(expectedList, result);
    }

    @Test
    void given_valid_task_id_should_return_task_assignments() {
        TaskAssignmentsRepository taskAssignmentsRepo = mock(TaskAssignmentsRepository.class);
        TaskAssignmentsResource assignment1 = mock(TaskAssignmentsResource.class);
        TaskAssignmentsResource assignment2 = mock(TaskAssignmentsResource.class);
        List<TaskAssignmentsResource> expectedList = newArrayList(assignment1, assignment2);

        when(taskAssignmentsRepo.findAllByTaskIdOrderByAssignmentIdAsc("task-123"))
            .thenReturn(expectedList);
        miReportingService = new MIReportingService(null, null,
            null, taskAssignmentsRepo, null);

        List<TaskAssignmentsResource> result = miReportingService.findByAssignmentsTaskId("task-123");

        assertEquals(2, result.size());
        assertEquals(expectedList, result);
    }

    // ============ Edge Case Tests for Count Values ============

    @Test
    void given_multiple_replication_slots_should_return_true() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countReplicationSlots()).thenReturn(5);
        miReportingService = new MIReportingService(null, taskResourceRepository,
            null, null, null);

        assertTrue(miReportingService.isReplicationSlotPresent());
    }

    @Test
    void given_multiple_publications_should_return_true() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countPublications()).thenReturn(3);
        miReportingService = new MIReportingService(null, taskResourceRepository,
            null, null, null);

        assertTrue(miReportingService.isPublicationPresent());
    }

    @Test
    void given_zero_publication_tables_should_return_true_for_work_types() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countPublicationTables()).thenReturn(0);
        miReportingService = new MIReportingService(null, taskResourceRepository,
            null, null, null);

        // When count is 0, it's not equal to 1 (PUBLICATION_ROW_COUNT_WITHOUT_WORK_TYPES)
        // so isWorkTypesInPublication returns true
        assertTrue(miReportingService.isWorkTypesInPublication());
    }

    @Test
    void given_multiple_publication_tables_should_return_true_for_work_types() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countPublicationTables()).thenReturn(5);
        miReportingService = new MIReportingService(null, taskResourceRepository,
            null, null, null);

        assertTrue(miReportingService.isWorkTypesInPublication());
    }

    @Test
    void given_multiple_subscriptions_should_return_true() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(3);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
            null, null, null);

        assertTrue(miReportingService.isSubscriptionPresent());
    }

    // ============ Exception Handling Tests ============

    @Test
    void given_null_taskResourceRepository_logicalReplicationCheck_should_throw_npe() {
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, null,
            null, null, null);

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> miReportingService.logicalReplicationCheck());

        assertEquals("Primary Task DB repo is null.", exception.getMessage());
    }

    @Test
    void given_null_taskHistoryRepository_logicalReplicationCheck_should_throw_npe() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.showWalLevel()).thenReturn("logical");

        miReportingService = new MIReportingService(null, taskResourceRepository,
            null, null, null);

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> miReportingService.logicalReplicationCheck());

        assertEquals("Replica Task DB repo is null.", exception.getMessage());
    }

    @Test
    void given_slot_creation_throws_exception_should_propagate() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.showWalLevel()).thenReturn("logical");
        when(taskResourceRepository.countReplicationSlots()).thenReturn(0);
        when(taskResourceRepository.createReplicationSlot()).thenThrow(new RuntimeException("Slot creation failed"));

        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, taskResourceRepository,
            null, null, null);

        assertThrows(RuntimeException.class, () -> miReportingService.logicalReplicationCheck());

        verify(taskResourceRepository, never()).createPublication();
    }

    @Test
    void given_publication_creation_throws_exception_should_propagate() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.showWalLevel()).thenReturn("logical");
        when(taskResourceRepository.countReplicationSlots()).thenReturn(1);
        when(taskResourceRepository.countPublications()).thenReturn(0);
        when(taskResourceRepository.createPublication()).thenThrow(new RuntimeException("Publication creation failed"));

        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, taskResourceRepository,
            null, null, null);

        assertThrows(RuntimeException.class, () -> miReportingService.logicalReplicationCheck());

        verify(taskHistoryResourceRepository, never()).countSubscriptions();
    }

    // ============ Idempotency Test ============

    @Test
    void given_replication_already_exists_logicalReplicationCheck_should_be_idempotent() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        TaskHistoryResourceRepository taskHistoryResourceRepository = mock(TaskHistoryResourceRepository.class);
        when(taskResourceRepository.showWalLevel()).thenReturn("logical");
        when(taskResourceRepository.countReplicationSlots()).thenReturn(1);
        when(taskResourceRepository.countPublications()).thenReturn(1);
        when(taskResourceRepository.countPublicationTables()).thenReturn(2); // work types present
        when(taskHistoryResourceRepository.countSubscriptions()).thenReturn(1);

        SubscriptionCreator subscriptionCreator = mock(SubscriptionCreator.class);
        miReportingService = new MIReportingService(taskHistoryResourceRepository, taskResourceRepository,
            null, null, subscriptionCreator);

        // Call multiple times to verify idempotency
        miReportingService.logicalReplicationCheck();
        miReportingService.logicalReplicationCheck();
        miReportingService.logicalReplicationCheck();

        // Should never create anything since all components already exist
        verify(taskResourceRepository, never()).createReplicationSlot();
        verify(taskResourceRepository, never()).createPublication();
        verify(taskResourceRepository, never()).addWorkTypesToPublication();
        verify(subscriptionCreator, never()).createSubscription();
        verify(subscriptionCreator, never()).refreshSubscription();
    }

}
