package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

public class AssignTaskRequest {

    private final String userId;

    public AssignTaskRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
