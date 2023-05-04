package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.ReportableTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportableTaskControllerTest {

    @Autowired
    ReportableTaskController reportableTaskController;

    @Mock
    MIReportingService miReportingService;

    @Before
    public void setup() {
        miReportingService = mock(MIReportingService.class);

        reportableTaskController = new ReportableTaskController(miReportingService);
    }

    @Test
    public void when_requested_a_reportable_task_object_returned() {
        String id = UUID.randomUUID().toString();
        ReportableTaskResource reportableTaskResource = new ReportableTaskResource();

        when(miReportingService.findByReportingTaskId(id)).thenReturn(Arrays.asList(reportableTaskResource));

        ReportableTaskResponse reportableTaskResponse = reportableTaskController.getReportableTask(id);

        assertEquals(reportableTaskResource, reportableTaskResponse.getReportableTaskList().get(0));
    }
}
