package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;

import static org.zalando.problem.Status.CONFLICT;

@SuppressWarnings("java:S110")
public class DatabaseConflictException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -8240657757666248400L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/database-conflict");
    private static final String TITLE = "Database Conflict Error";

    public DatabaseConflictException(ErrorMessages message) {
        super(TYPE, TITLE, CONFLICT, message.getDetail());
    }
}
