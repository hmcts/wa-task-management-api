package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation;

import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import java.net.URI;
import java.util.List;

import static org.zalando.problem.Status.BAD_REQUEST;

@SuppressWarnings("java:S110")
public class CustomConstraintViolationException extends ConstraintViolationProblem {

    private static final long serialVersionUID = -5095055075702852131L;
    private static final URI TYPE =
        URI.create("https://github.com/hmcts/wa-task-management-api/problem/constraint-validation");

    public CustomConstraintViolationException(List<Violation> violations) {
        super(TYPE, BAD_REQUEST, violations);
    }
}
