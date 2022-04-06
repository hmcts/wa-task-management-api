package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;

import java.net.URI;

import static org.zalando.problem.Status.BAD_REQUEST;

@SuppressWarnings("java:S110")
public class InvalidRequestException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -7304118612753159516L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/bad-request");
    private static final String TITLE = "Bad Request";

    public InvalidRequestException(String message) {
        super(TYPE, TITLE, BAD_REQUEST, message);
    }
}
