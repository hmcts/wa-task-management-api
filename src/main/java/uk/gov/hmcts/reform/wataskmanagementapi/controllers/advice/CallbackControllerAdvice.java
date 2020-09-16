package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ControllerAdvice(basePackages = "uk.gov.hmcts.reform.wataskmanagementapi.controllers")
@RequestMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
public class CallbackControllerAdvice extends ResponseEntityExceptionHandler {

    private final ErrorLogger errorLogger;

    @Autowired
    public CallbackControllerAdvice(@Autowired ErrorLogger errorLogger) {
        super();
        this.errorLogger = errorLogger;
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<String> handleGenericException(
        HttpServletRequest request,
        Exception ex
    ) {
        errorLogger.maybeLogException(ex);
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<String> handleResourceNotFoundException(
        HttpServletRequest request,
        Exception ex
    ) {
        errorLogger.maybeLogException(ex);
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

}
