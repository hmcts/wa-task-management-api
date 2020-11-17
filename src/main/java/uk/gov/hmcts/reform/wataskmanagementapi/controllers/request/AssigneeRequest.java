package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AssigneeRequest {
    private final String userId;

    public AssigneeRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
