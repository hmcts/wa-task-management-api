package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.spring.web.advice.validation.ValidationAdviceTrait;
import org.zalando.problem.violations.Violation;

import java.util.List;
import javax.validation.ConstraintViolationException;

import static java.util.stream.Collectors.toList;

//Example of overriding a Trait
@ControllerAdvice
public class ConstraintViolationAdvice implements ValidationAdviceTrait {

    @Override
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Problem> handleConstraintViolation(
        final ConstraintViolationException exception,
        final NativeWebRequest request) {

        final List<Violation> violations = exception.getConstraintViolations().stream()
            .map(this::createViolation)
            .collect(toList());
        violations.add(new Violation(
            "overriddenViolation",
            "overridden violation from ConstraintViolationAdviceTrait"
        ));

        return newConstraintViolationProblem(exception, violations, request);
    }


}
