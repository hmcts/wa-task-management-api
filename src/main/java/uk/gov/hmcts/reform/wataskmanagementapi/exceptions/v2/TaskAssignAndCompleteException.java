package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

public class TaskAssignAndCompleteException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -7304118612753159016L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-assign-and-complete-error");
    private static final String TITLE = "Task Assign and Complete Error";

    public TaskAssignAndCompleteException(ErrorMessages message) {
        super(TYPE, TITLE, INTERNAL_SERVER_ERROR, message.getDetail());
    }
}
