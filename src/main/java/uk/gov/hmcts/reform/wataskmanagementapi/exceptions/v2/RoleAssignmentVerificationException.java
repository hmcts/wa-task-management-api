package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;

import static org.zalando.problem.Status.FORBIDDEN;

@SuppressWarnings("java:S110")
public class RoleAssignmentVerificationException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -4059403052733143616L;

    private static final URI TYPE = URI.create("https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure");
    private static final String TITLE = "Role Assignment Verification";

    public RoleAssignmentVerificationException(ErrorMessages message) {
        super(TYPE, TITLE, FORBIDDEN, message.getDetail());
    }
}
