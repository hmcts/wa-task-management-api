package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import feign.FeignException;
import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
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
        Throwable rootCause = findRootCause(cause);
        this.diagnosticType = findDiagnosticType(cause, rootCause);
        this.diagnosticLocation = findDiagnosticLocation(rootCause, cause);
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

    private static Throwable findRootCause(Throwable cause) {
        Throwable current = cause;
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(current);
        while (current.getCause() != null && seen.add(current.getCause())) {
            current = current.getCause();
        }
        return current;
    }

    private static String findDiagnosticType(Throwable cause, Throwable rootCause) {
        if (rootCause instanceof FeignException feignException) {
            String type = cause.getClass().getSimpleName() + " -> " + rootCause.getClass().getSimpleName();
            return feignException.status() < 0 ? type : type + " (HTTP " + feignException.status() + ")";
        }
        return cause.getClass().getSimpleName() + " -> " + rootCause.getClass().getSimpleName();
    }

    private static String findDiagnosticLocation(Throwable rootCause, Throwable originalCause) {
        String location = findApplicationFrame(rootCause);
        return location == null ? findApplicationFrame(originalCause) : location;
    }

    private static String findApplicationFrame(Throwable cause) {
        for (StackTraceElement frame : cause.getStackTrace()) {
            if (frame.getClassName().startsWith("uk.gov.hmcts.reform.wataskmanagementapi.")) {
                return frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber();
            }
        }
        return null;
    }
}
