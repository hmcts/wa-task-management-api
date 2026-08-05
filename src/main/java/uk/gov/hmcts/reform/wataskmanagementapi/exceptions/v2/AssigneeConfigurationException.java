package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import org.zalando.problem.AbstractThrowableProblem;

import java.net.URI;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@SuppressWarnings("java:S110")
public class AssigneeConfigurationException extends AbstractThrowableProblem {

    private static final long serialVersionUID = 3413789972854759388L;
    private static final URI TYPE = URI.create(
        "https://github.com/hmcts/wa-task-management-api/problem/assignee-configuration-error"
    );
    private static final String TITLE = "Assignee Configuration Error";

    public AssigneeConfigurationException(String message) {
        super(TYPE, TITLE, INTERNAL_SERVER_ERROR, message);
    }
}
