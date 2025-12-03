package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class TaskDeletionServiceTest {

    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;

    @InjectMocks
    private TaskDeletionService taskDeletionService;

    @Test
    void shouldDeletedTasksResponse() {
        final String caseId = "123";
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder1 = mock(TaskResourceCaseQueryBuilder.class);
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder2 = mock(TaskResourceCaseQueryBuilder.class);

        when(cftTaskDatabaseService.findByTaskIdsByCaseId(caseId)).thenReturn(List.of(taskResourceCaseQueryBuilder1,
                taskResourceCaseQueryBuilder2));

        when(taskResourceCaseQueryBuilder1.getTaskId()).thenReturn("234");
        when(taskResourceCaseQueryBuilder2.getTaskId()).thenReturn("567");

        when(taskResourceCaseQueryBuilder1.getState()).thenReturn(CFTTaskState.TERMINATED);
        when(taskResourceCaseQueryBuilder2.getState()).thenReturn(CFTTaskState.CANCELLED);

        doNothing().when(cftTaskDatabaseService).deleteTasks(List.of("234", "567"));

        taskDeletionService.deleteTasksByCaseId(caseId);

        verify(cftTaskDatabaseService, times(1)).findByTaskIdsByCaseId("123");
        verify(cftTaskDatabaseService, times(1)).deleteTasks(List.of("234", "567"));
    }

    @Test
    void shouldNotDeleteTasks(CapturedOutput output) {
        final String caseId = "123";
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder1 = mock(TaskResourceCaseQueryBuilder.class);

        when(cftTaskDatabaseService.findByTaskIdsByCaseId(caseId)).thenReturn(List.of(taskResourceCaseQueryBuilder1));

        when(taskResourceCaseQueryBuilder1.getTaskId()).thenReturn("234");
        when(taskResourceCaseQueryBuilder1.getState()).thenReturn(CFTTaskState.TERMINATED);
        doThrow(new RuntimeException("some exception")).when(cftTaskDatabaseService).deleteTasks(List.of("234"));

        taskDeletionService.deleteTasksByCaseId(caseId);

        assertThat(output.getOut().contains(String.format("Unable to delete all tasks for case id: %s",
                caseId)));

        assertThat(output.getOut().contains(String.format("Exception occurred:: %s", "some exception")));
    }

    //TODO implement markTasksToDeleteByCaseId positive test method for new method in TasksDeletionService


}