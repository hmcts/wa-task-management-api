package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.launchdarkly.shaded.com.google.common.base.CaseFormat;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import org.zalando.problem.spring.web.advice.validation.ValidationAdviceTrait;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.DatabaseConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.InvalidRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAlreadyClaimedException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskClaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskReconfigurationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskUnclaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomizedConstraintViolationException;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.SERVICE_UNAVAILABLE;

@Slf4j
@ControllerAdvice(basePackages = {
    "uk.gov.hmcts.reform.wataskmanagementapi.controllers",
    "uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers"
})
@RequestMapping(produces = APPLICATION_PROBLEM_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.DataflowAnomalyAnalysis",
    "PMD.UseStringBufferForStringAppends", "PMD.LawOfDemeter", "PMD.CouplingBetweenObjects",
    "PMD.TooManyMethods", "PMD.CognitiveComplexity"})
public class ApplicationProblemControllerAdvice extends BaseControllerAdvice implements ValidationAdviceTrait {

    @ExceptionHandler({
        FeignException.class,
        ServerErrorException.class,
    })
    public ResponseEntity<ThrowableProblem> handleFeignAndServerException(FeignException ex) {
        log.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);

        Status statusType = BAD_GATEWAY; //502
        URI type = URI.create("https://github.com/hmcts/wa-task-management-api/problem/downstream-dependency-error");
        String title = "Downstream Dependency Error";
        ErrorMessages detail = ErrorMessages.DOWNSTREAM_DEPENDENCY_ERROR;

        return ResponseEntity.status(statusType.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(new CustomProblem(type, statusType.getStatusCode(), title,
                                    detail.getDetail()
                                        + " Message from downstream system: "
                                        + ex.getMessage()));
    }

    @ExceptionHandler({
        FeignException.ServiceUnavailable.class,
        FeignException.GatewayTimeout.class
    })
    public ResponseEntity<ThrowableProblem> handleServiceUnavailableException(FeignException ex) {
        log.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);

        Status statusType = SERVICE_UNAVAILABLE; //503
        URI type = URI.create("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable");
        String title = "Service Unavailable";
        ErrorMessages detail = ErrorMessages.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(statusType.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(new CustomProblem(type, statusType.getStatusCode(), title, detail.getDetail()));
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
            .body(new CustomProblem(type, statusType.getStatusCode(), title, detail.getDetail()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Problem> handleMessageNotReadable(HttpMessageNotReadableException exception) {

        Status statusType = BAD_REQUEST; //400
        URI type = URI.create("https://github.com/hmcts/wa-task-management-api/problem/bad-request");
        String title = "Bad Request";

        String errorMessage = extractErrors(exception);
        return ResponseEntity.status(statusType.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(new CustomProblem(type, statusType.getStatusCode(), title, errorMessage));
    }

    @ExceptionHandler({
        CustomConstraintViolationException.class,
        CustomizedConstraintViolationException.class
    })
    public ResponseEntity<Problem> handleConstraintViolation(ConstraintViolationProblem ex) {

        return ResponseEntity.status(ex.getStatus().getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(new CustomConstraintViolationProblem(
                ex.getType(),
                ex.getStatus(),
                ex.getViolations(),
                ex.getStatus().getStatusCode())
            );
    }

    @Override
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Problem> handleConstraintViolation(
        ConstraintViolationException ex,
        NativeWebRequest request) {
        Status status = BAD_REQUEST; //400
        URI type = URI.create("https://github.com/hmcts/wa-task-management-api/problem/constraint-validation");

        final List<Violation> violations = ex.getConstraintViolations().stream()
            .map(this::createViolation)
            .toList();

        return ResponseEntity.status(status.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(new CustomConstraintViolationProblem(
                type,
                status,
                violations,
                status.getStatusCode())
            );
    }

    @Override
    public String formatFieldName(String fieldName) {
        return asSnakeCaseString(fieldName);
    }

    @Override
    public ResponseEntity<Problem> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                NativeWebRequest request) {

        Status status = BAD_REQUEST; //400
        URI type = URI.create("https://github.com/hmcts/wa-task-management-api/problem/constraint-validation");

        List<Violation> streamViolations = createViolations(ex.getBindingResult());

        List<Violation> violations = streamViolations.stream()
            // sorting to make tests deterministic
            .sorted(comparing(Violation::getField).thenComparing(Violation::getMessage))
            .toList();

        return ResponseEntity.status(status.getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(new CustomConstraintViolationProblem(
                type,
                status,
                violations,
                status.getStatusCode())
            );

    }

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
        GenericServerErrorException.class,
        TaskNotFoundException.class,
        InvalidRequestException.class,
        TaskReconfigurationException.class,
        TaskAlreadyClaimedException.class
    })
    protected ResponseEntity<Problem> handleApplicationProblemExceptions(
        AbstractThrowableProblem ex
    ) {
        log.error(EXCEPTION_OCCURRED, ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus().getStatusCode())
            .header(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
            .body(new CustomProblem(ex.getType(), ex.getStatus().getStatusCode(), ex.getTitle(), ex.getMessage()));
    }

    /**
     * Common handling of JSON parsing/mapping exceptions.Avoids having to return error
     * details with internal Java package/class names.
     */
    private String extractErrors(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();

        if (cause instanceof JsonParseException jpe) {
            return handleJsonParseException(jpe);
        } else if (cause instanceof MismatchedInputException mie) {
            return handleMismatchedInputException(mie);
        } else if (cause instanceof JsonMappingException jme) {
            return handleJsonMappingException(jme);
        } else {
            return "Invalid request message";
        }
    }

    private String handleJsonParseException(JsonParseException jpe) {
        return jpe.getOriginalMessage();
    }

    private String handleMismatchedInputException(MismatchedInputException mie) {
        if (mie.getPath() != null && !mie.getPath().isEmpty()) {
            String fieldName = extractFieldName(mie.getPath());
            return "Invalid request field: " + fieldName;
        }
        return "Invalid request message";
    }

    private String handleJsonMappingException(JsonMappingException jme) {
        String fieldName = extractFieldName(jme.getPath());
        String originalMessage = jme.getOriginalMessage();
        return "Invalid request field: " + fieldName + ": " + originalMessage;
    }

    private String extractFieldName(List<JsonMappingException.Reference> path) {
        return path.stream()
            .map(ref -> ref.getFieldName() == null ? "[0]" : ref.getFieldName())
            .collect(Collectors.joining("."));
    }

    /**
     * Convert field names to snake case for ConstraintValidations.
     *
     * @param fieldName the camel cased field name.
     * @return the field name as snake case.
     */
    private static String asSnakeCaseString(String fieldName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
    }
}
