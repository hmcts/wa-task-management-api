package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskHistoryResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskHistoryControllerTest {

    @Autowired
    TaskHistoryController taskHistoryController;

    @Mock
    MIReportingService miReportingService;

    @Before
    public void setup() {
        miReportingService = mock(MIReportingService.class);

        taskHistoryController = new TaskHistoryController(miReportingService);
    }

    @Test
    public void when_requested_a_task_history_object_returned() {
        String id = UUID.randomUUID().toString();
        TaskHistoryResource taskHistoryResource = new TaskHistoryResource();

        when(miReportingService.findByTaskId(id)).thenReturn(Arrays.asList(taskHistoryResource));

        TaskHistoryResponse taskHistoryResponse = taskHistoryController.getTaskHistory(id);

        assertEquals(taskHistoryResource, taskHistoryResponse.getTaskHistoryList().get(0));
    }
}
