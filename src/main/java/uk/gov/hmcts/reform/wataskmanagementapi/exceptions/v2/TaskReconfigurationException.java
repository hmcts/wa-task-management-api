package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.zalando.problem.Status.CONFLICT;

public class TaskReconfigurationException extends AbstractThrowableProblem {

    private static final long serialVersionUID = 1L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-reconfiguration-error");
    private static final String TITLE = "Task Reconfiguration Failed";

    public TaskReconfigurationException(ErrorMessages message, List<String> caseIds) {
        super(TYPE, TITLE, CONFLICT, message.getDetail() + caseIds.stream().collect(Collectors.joining(",")));
    }
}