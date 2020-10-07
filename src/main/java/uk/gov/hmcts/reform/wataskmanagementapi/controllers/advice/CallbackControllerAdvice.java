package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ControllerAdvice(basePackages = "uk.gov.hmcts.reform.wataskmanagementapi.controllers")
@RequestMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
public class CallbackControllerAdvice extends ResponseEntityExceptionHandler {

    private final ErrorLogger errorLogger;
    private final SystemDateProvider systemDateProvider;

    @Autowired
    public CallbackControllerAdvice(
        ErrorLogger errorLogger,
        SystemDateProvider systemDateProvider
    ) {
        super();
        this.errorLogger = errorLogger;
        this.systemDateProvider = systemDateProvider;
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorMessage> handleGenericException(
        HttpServletRequest request,
        Exception ex
    ) {
        errorLogger.maybeLogException(ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.SERVICE_UNAVAILABLE,
                      systemDateProvider.nowWithTime()
                  )
            );
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<ErrorMessage> handleResourceNotFoundException(
        HttpServletRequest request,
        Exception ex
    ) {
        errorLogger.maybeLogException(ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.NOT_FOUND,
                      systemDateProvider.nowWithTime()
                  )
            );
    }


    @ExceptionHandler(ServerErrorException.class)
    protected ResponseEntity<ErrorMessage> handleServerException(
        HttpServletRequest request,
        Exception ex
    ) {
        errorLogger.maybeLogException(ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.INTERNAL_SERVER_ERROR,
                      systemDateProvider.nowWithTime()
                  )
            );
    }

    @ExceptionHandler(NotImplementedException.class)
    protected ResponseEntity<ErrorMessage> handleNotImplementedException(
        HttpServletRequest request,
        Exception ex
    ) {
        errorLogger.maybeLogException(ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.SERVICE_UNAVAILABLE,
                      systemDateProvider.nowWithTime()
                  )
            );
    }

}
