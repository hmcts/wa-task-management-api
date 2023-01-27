package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class ReplicationException extends RuntimeException {

    private static final long serialVersionUID = -8802821685136238535L;

    public ReplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
