package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.zalando.problem.Status.CONFLICT;

@SuppressWarnings("java:S110")
public class TaskExecuteReconfigurationException extends AbstractThrowableProblem {

    private static final long serialVersionUID = 1L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-reconfiguration-error");
    private static final String TITLE = "Task Execute Reconfiguration Failed";

    public TaskExecuteReconfigurationException(ErrorMessages message, List<TaskResource> taskResources) {

        super(TYPE, TITLE, CONFLICT, message.getDetail() + taskResources.stream()
            .map(task -> "\n" + task.getTaskId()
                + " ," + task.getTaskName()
                + " ," + task.getState()
                + " ," + task.getReconfigureRequestTime()
                + " ," + task.getLastReconfigurationTime())
            .collect(Collectors.joining()));
    }
}
