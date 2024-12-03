package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.zalando.problem.StatusType;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import java.net.URI;
import java.util.List;

public class CustomConstraintViolationProblem extends ConstraintViolationProblem {

    private final int statusCode;

    public CustomConstraintViolationProblem(URI type, StatusType status, List<Violation> violations, int statusCode) {
        super(type, status, violations);
        this.statusCode = statusCode;
    }

    @Nullable
    @Override
    @JsonIgnore
    @JsonProperty("statusType")
    public StatusType getStatus() {
        return this.getStatus();
    }

    @JsonProperty("status")
    public int getStatusCode() {
        return this.statusCode;
    }
}
