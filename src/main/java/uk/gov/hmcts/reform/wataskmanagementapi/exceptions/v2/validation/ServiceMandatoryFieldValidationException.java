package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation;

public class ServiceMandatoryFieldValidationException extends RuntimeException {

    private static final long serialVersionUID = -5095055075702852145L;

    public ServiceMandatoryFieldValidationException(String message) {
        super(message);
    }

    public ServiceMandatoryFieldValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
