package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;

import static org.zalando.problem.Status.NOT_FOUND;

@SuppressWarnings("java:S110")
public class TaskNotFoundException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -7304118612753159016L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error");
    private static final String TITLE = "Task Not Found Error";

    public TaskNotFoundException(ErrorMessages message) {
        super(TYPE, TITLE, NOT_FOUND, message.getDetail());
    }
}
