package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;
import java.util.UUID;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@SuppressWarnings("java:S110")
public class GenericServerErrorException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -5169653220966292495L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/generic-server-error");
    private static final String TITLE = "Generic Server Error";

    private final String diagnosticId;
    private final String diagnosticType;
    private final String diagnosticLocation;

    public GenericServerErrorException(ErrorMessages message) {
        super(TYPE, TITLE, INTERNAL_SERVER_ERROR, message.getDetail());
        this.diagnosticId = null;
        this.diagnosticType = null;
        this.diagnosticLocation = null;
    }

    public GenericServerErrorException(ErrorMessages message, Throwable cause) {
        super(TYPE, TITLE, INTERNAL_SERVER_ERROR, message.getDetail());
        this.diagnosticId = UUID.randomUUID().toString();
        this.diagnosticType = cause.getClass().getSimpleName();
        this.diagnosticLocation = findDiagnosticLocation(cause);
    }

    public String getDiagnosticId() {
        return diagnosticId;
    }

    public String getDiagnosticType() {
        return diagnosticType;
    }

    public String getDiagnosticLocation() {
        return diagnosticLocation;
    }

    private static String findDiagnosticLocation(Throwable cause) {
        for (StackTraceElement frame : cause.getStackTrace()) {
            if (frame.getClassName().startsWith("uk.gov.hmcts.reform.wataskmanagementapi.")) {
                return frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber();
            }
        }
        return "unknown";
    }
}
