package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class DateCalculationException extends RuntimeException {

    private static final long serialVersionUID = -8209448175909828375L;

    public DateCalculationException(String message) {
        super(message);
    }

    public DateCalculationException(String message, Throwable cause) {
        super(message, cause);
    }

}
