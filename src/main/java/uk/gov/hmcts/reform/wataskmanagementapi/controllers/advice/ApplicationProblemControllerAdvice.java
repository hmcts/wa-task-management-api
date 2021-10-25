package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
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
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.SERVICE_UNAVAILABLE;

@Slf4j
@ControllerAdvice(basePackages = {
    "uk.gov.hmcts.reform.wataskmanagementapi.controllers",
    "uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers"
})
@RequestMapping(produces = APPLICATION_PROBLEM_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
public class ApplicationProblemControllerAdvice extends BaseControllerAdvice {


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

    @ExceptionHandler(FeignException.ServiceUnavailable.class)
    public ResponseEntity<ThrowableProblem> handleFeignServiceUnavailableException(FeignException ex) {
        log.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);

        Status statusType = BAD_GATEWAY; //502
        URI type = URI.create("https://github.com/hmcts/wa-task-management-api/problem/downstream-dependency-error");
        String title = "Downstream Dependency Error";
        ErrorMessages detail = ErrorMessages.DOWNSTREAM_DEPENDENCY_ERROR;

        return ResponseEntity.status(statusType.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(Problem.builder()
                .withType(type)
                .withTitle(title)
                .withDetail(detail.getDetail())
                .withStatus(statusType)
                .build());
    }

    @ExceptionHandler(JDBCConnectionException.class)
    public ResponseEntity<ThrowableProblem> handleJdbcConnectionException(Exception ex) {
        log.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);

        Status statusType = SERVICE_UNAVAILABLE; //503
        URI type = URI.create("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable");
        String title = "Service Unavailable";
        ErrorMessages detail = ErrorMessages.DATABASE_IS_UNAVAILABLE;

        return ResponseEntity.status(statusType.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(Problem.builder()
                .withType(type)
                .withTitle(title)
                .withDetail(detail.getDetail())
                .withStatus(statusType)
                .build());
    }


}
