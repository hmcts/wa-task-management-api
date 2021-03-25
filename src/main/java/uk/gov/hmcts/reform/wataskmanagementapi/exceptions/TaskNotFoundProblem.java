package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

import java.net.URI;

public class TaskNotFoundProblem extends AbstractThrowableProblem {

    private static final URI TYPE
        = URI.create("https://task-manager/not-found");
    private static final String TITLE = "Task Not Found";

    public TaskNotFoundProblem(String detail) {
        super(TYPE, TITLE,
              Status.NOT_FOUND,
              detail
        );
    }
}
