package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class AssignTaskRequest {

    private final String userId;

    @JsonCreator
    public AssignTaskRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
