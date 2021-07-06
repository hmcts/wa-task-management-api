package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.BadRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ControllerAdvice(basePackages = "uk.gov.hmcts.reform.wataskmanagementapi.controllers")
@RequestMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
public class CallbackControllerAdvice extends ResponseEntityExceptionHandler {

    public static final String EXCEPTION_OCCURRED = "Exception occurred: {}";
    private static final Logger LOG = getLogger(CallbackControllerAdvice.class);
    private final SystemDateProvider systemDateProvider;

    @Autowired
    public CallbackControllerAdvice(
        SystemDateProvider systemDateProvider
    ) {
        super();
        this.systemDateProvider = systemDateProvider;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<ErrorMessage> handleResourceNotFoundException(
        Exception ex
    ) {
        LOG.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.NOT_FOUND,
                      systemDateProvider.nowWithTime()
                  )
            );
    }

    @ExceptionHandler(ConflictException.class)
    protected ResponseEntity<ErrorMessage> handleConflictException(
        Exception ex
    ) {
        LOG.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.CONFLICT,
                      systemDateProvider.nowWithTime()
                  )
            );
    }

    @ExceptionHandler(NotImplementedException.class)
    protected ResponseEntity<ErrorMessage> handleNotImplementedException(
        Exception ex
    ) {
        LOG.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.SERVICE_UNAVAILABLE,
                      systemDateProvider.nowWithTime()
                  )
            );
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    protected ResponseEntity<ErrorMessage> handleUnsupportedOperationException(
        Exception ex
    ) {
        LOG.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.BAD_REQUEST,
                      systemDateProvider.nowWithTime()
                  )
            );
    }

    @ExceptionHandler(UnAuthorizedException.class)
    protected ResponseEntity<ErrorMessage> handleUnAuthorizedException(
        Exception ex
    ) {
        LOG.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.UNAUTHORIZED,
                      systemDateProvider.nowWithTime()
                  )
            );
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<ErrorMessage> handleBadRequestsException(
        Exception ex
    ) {
        LOG.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.BAD_REQUEST,
                      systemDateProvider.nowWithTime()
                  )
            );
    }

    @ExceptionHandler(TaskStateIncorrectException.class)
    protected ResponseEntity<ErrorMessage> handleTaskStateIncorrectExceptionException(
        Exception ex
    ) {
        LOG.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.FORBIDDEN,
                      systemDateProvider.nowWithTime()
                  )
            );
    }

    @ExceptionHandler({ServerErrorException.class})
    protected ResponseEntity<ErrorMessage> handleGenericException(
        Exception ex
    ) {
        LOG.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorMessage(
                      ex,
                      HttpStatus.INTERNAL_SERVER_ERROR,
                      systemDateProvider.nowWithTime()
                  )
            );
    }
}
