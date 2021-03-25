package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.spring.web.advice.ProblemHandling;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.InsufficientPermissionsException;

import java.net.URI;

@Slf4j
@ControllerAdvice
//Generic ProblemHandlingTrait
class ExceptionHandling implements ProblemHandling {

    public static final String EXCEPTION_OCCURRED = "Exception occurred: {}";

    private static final URI URI = java.net.URI.create("https://task-manager/failed-role-assignment-verification");

    @ExceptionHandler(InsufficientPermissionsException.class)
    protected ResponseEntity<Problem> handleInsufficientPermissionsException(
        Exception ex
    ) {
        log.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(Status.FORBIDDEN.getStatusCode())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .body(Problem.builder()
                      .withType(URI)
                      .withTitle("Role Assignment Verification")
                      .withDetail("Failed role assignment verifications")
                      .withStatus(Status.FORBIDDEN)
                      .build());
    }

}
