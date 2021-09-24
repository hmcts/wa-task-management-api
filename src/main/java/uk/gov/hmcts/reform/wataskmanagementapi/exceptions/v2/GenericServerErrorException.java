package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@SuppressWarnings("java:S110")
public class GenericServerErrorException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -5169653220966292495L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/generic-server-error");
    private static final String TITLE = "Generic Server Error";

    public GenericServerErrorException(ErrorMessages message) {
        super(TYPE, TITLE, INTERNAL_SERVER_ERROR, message.getDetail());
    }
}
