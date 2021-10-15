package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Problem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.DatabaseConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskClaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskUnclaimException;

import java.net.URI;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.zalando.problem.Status.SERVICE_UNAVAILABLE;

@Slf4j
@ControllerAdvice(basePackages = {
    "uk.gov.hmcts.reform.wataskmanagementapi.controllers",
    "uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers"
})
@RequestMapping(produces = APPLICATION_PROBLEM_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
public class ApplicationProblemControllerAdvice extends BaseControllerAdvice {

    private static final URI SERVICE_UNAVAILABLE_PROBLEM_URI = URI.create("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable");
    private static final String SERVICE_UNAVAILABLE_PROBLEM_TITLE = "Service Unavailable";
    private static final String DATABASE_UNAVAILABLE_ERROR = "Database is unavailable.";

    @ExceptionHandler({
        GenericForbiddenException.class,
        RoleAssignmentVerificationException.class,
        TaskAssignAndCompleteException.class,
        TaskAssignException.class,
        TaskClaimException.class,
        TaskCompleteException.class,
        TaskUnclaimException.class,
        TaskCancelException.class,
        DatabaseConflictException.class,
        GenericServerErrorException.class
    })
    protected ResponseEntity<Problem> handleApplicationProblemExceptions(
        AbstractThrowableProblem ex
    ) {
        log.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus().getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(Problem.builder()
                      .withType(ex.getType())
                      .withTitle(ex.getTitle())
                      .withDetail(ex.getMessage())
                      .withStatus(ex.getStatus())
                      .build());
    }

    @ExceptionHandler(JDBCConnectionException.class)
    protected ResponseEntity<Problem> handleJdbcConnectionExceptions(JDBCConnectionException ex) {
        log.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(Problem.builder()
                .withType(SERVICE_UNAVAILABLE_PROBLEM_URI)
                .withTitle(SERVICE_UNAVAILABLE_PROBLEM_TITLE)
                .withDetail(DATABASE_UNAVAILABLE_ERROR)
                .withStatus(SERVICE_UNAVAILABLE)
                .build());
    }

}
