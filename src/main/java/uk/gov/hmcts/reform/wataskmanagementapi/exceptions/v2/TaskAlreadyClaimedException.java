package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Problem;

import java.net.URI;

import static org.zalando.problem.Status.CONFLICT;

@SuppressWarnings("java:S110")
public class TaskAlreadyClaimedException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -7314118612753159016L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-already-claimed-error");
    private static final String TITLE = "Task Already Claimed Error";
    
    public TaskAlreadyClaimedException(String message, Exception ex) {
        super(TYPE, TITLE, CONFLICT, message, null, Problem.valueOf(CONFLICT, ex.getMessage()));
    }

}
