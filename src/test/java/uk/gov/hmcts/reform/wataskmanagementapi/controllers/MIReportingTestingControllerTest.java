package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.ReportableTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskAssignmentsResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskHistoryResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskAssignmentsResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MIReportingTestingControllerTest {

    @Autowired
    MIReportingTestingController miReportingTestingController;

    @Mock
    MIReportingService miReportingService;

    @Before
    public void setup() {
        miReportingService = mock(MIReportingService.class);

        miReportingTestingController = new MIReportingTestingController(miReportingService);
    }

    @Test
    public void when_requested_a_task_history_object_returned() {
        String id = UUID.randomUUID().toString();
        TaskHistoryResource taskHistoryResource = new TaskHistoryResource();

        when(miReportingService.findByTaskId(id)).thenReturn(Arrays.asList(taskHistoryResource));

        TaskHistoryResponse taskHistoryResponse = miReportingTestingController.getTaskHistory(id);

        assertEquals(taskHistoryResource, taskHistoryResponse.getTaskHistoryList().get(0));
    }

    @Test
    public void when_requested_a_reportable_task_object_returned() {
        String id = UUID.randomUUID().toString();
        ReportableTaskResource reportableTaskResource = new ReportableTaskResource();

        when(miReportingService.findByReportingTaskId(id)).thenReturn(Arrays.asList(reportableTaskResource));

        ReportableTaskResponse reportableTaskResponse = miReportingTestingController.getReportableTask(id);

        assertEquals(reportableTaskResource, reportableTaskResponse.getReportableTaskList().get(0));
    }

    @Test
    public void when_requested_a_task_assignment_object_returned() {
        String id = UUID.randomUUID().toString();
        TaskAssignmentsResource taskAssignmentsResource = new TaskAssignmentsResource();

        when(miReportingService.findByAssignmentsTaskId(id)).thenReturn(Arrays.asList(taskAssignmentsResource));

        TaskAssignmentsResponse taskAssignmentsResponse = miReportingTestingController.getTaskAssignments(id);

        assertEquals(taskAssignmentsResource, taskAssignmentsResponse.getTaskAssignmentsList().get(0));
    }


    @Test
    public void should_throw_error_when_mi_history_endpoints_called_in_prod() {

        ReflectionTestUtils.setField(miReportingTestingController, "environment", "prod");
        String id = UUID.randomUUID().toString();

        assertThatThrownBy(() -> miReportingTestingController.getTaskHistory(id)).isInstanceOf(
            GenericForbiddenException.class);
    }

    @Test
    public void should_throw_error_when_mi_reportable_endpoints_called_in_prod() {

        ReflectionTestUtils.setField(miReportingTestingController, "environment", "prod");
        String id = UUID.randomUUID().toString();

        assertThatThrownBy(() -> miReportingTestingController.getReportableTask(id)).isInstanceOf(
            GenericForbiddenException.class);
    }

}
