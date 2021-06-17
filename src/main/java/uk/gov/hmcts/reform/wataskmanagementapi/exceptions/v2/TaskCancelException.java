package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

public class TaskCancelException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -3202537017930376522L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-cancel-error");
    private static final String TITLE = "Task Cancel Error";

    public TaskCancelException(ErrorMessages message) {
        super(TYPE, TITLE, INTERNAL_SERVER_ERROR, message.getDetail());
    }
}
