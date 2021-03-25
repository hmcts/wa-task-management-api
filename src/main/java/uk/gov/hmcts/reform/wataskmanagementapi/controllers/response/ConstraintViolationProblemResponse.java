package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import org.zalando.problem.violations.Violation;

import java.util.List;

public class ConstraintViolationProblemResponse {

    private final String type;
    private final String title;
    private final String status;
    private final List<Violation> violations;

    public ConstraintViolationProblemResponse(
        String type,
        String title,
        String status,
        List<Violation> violations
    ) {

        this.type = type;
        this.title = title;
        this.status = status;
        this.violations = violations;

    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
