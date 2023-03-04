package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class InvalidDateTypeConfigurationException extends RuntimeException {

    private static final long serialVersionUID = -8209448175909828375L;

    public InvalidDateTypeConfigurationException(String message) {
        super(message);
    }

    public InvalidDateTypeConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
