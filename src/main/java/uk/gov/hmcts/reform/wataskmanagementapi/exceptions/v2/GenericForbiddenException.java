package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;

import static org.zalando.problem.Status.FORBIDDEN;

@SuppressWarnings("java:S110")
public class GenericForbiddenException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -4084778629910715656L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/forbidden");
    private static final String TITLE = "Forbidden";

    public GenericForbiddenException(ErrorMessages message) {
        super(TYPE, TITLE, FORBIDDEN, message.getDetail());
    }
}
