package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import org.apache.commons.lang3.NotImplementedException;
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
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.RequireDbLockException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ControllerAdvice(
    basePackages = {
        "uk.gov.hmcts.reform.wataskmanagementapi.controllers",
        "uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers"
    })
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

    @ExceptionHandler({ResourceNotFoundException.class})
    protected ResponseEntity<ErrorMessage> handleResourceNotFoundException(Exception ex) {
        return getErrorMessageResponseEntity(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    protected ResponseEntity<ErrorMessage> handleConflictException(Exception ex) {
        return getErrorMessageResponseEntity(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NotImplementedException.class)
    protected ResponseEntity<ErrorMessage> handleNotImplementedException(Exception ex) {
        return getErrorMessageResponseEntity(ex, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    protected ResponseEntity<ErrorMessage> handleUnsupportedOperationException(Exception ex) {
        return getErrorMessageResponseEntity(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnAuthorizedException.class)
    protected ResponseEntity<ErrorMessage> handleUnAuthorizedException(Exception ex) {
        return getErrorMessageResponseEntity(ex, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<ErrorMessage> handleBadRequestsException(Exception ex) {
        return getErrorMessageResponseEntity(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TaskStateIncorrectException.class)
    protected ResponseEntity<ErrorMessage> handleTaskStateIncorrectExceptionException(Exception ex) {
        return getErrorMessageResponseEntity(ex, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({NullPointerException.class, ServerErrorException.class,
        RequireDbLockException.class})
    protected ResponseEntity<ErrorMessage> handleGenericException(Exception ex) {
        return getErrorMessageResponseEntity(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorMessage> getErrorMessageResponseEntity(Exception ex, HttpStatus httpStatus) {
        LOG.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(httpStatus)
            .body(new ErrorMessage(
                    ex,
                    httpStatus,
                    systemDateProvider.nowWithTime()
                )
            );
    }
}
