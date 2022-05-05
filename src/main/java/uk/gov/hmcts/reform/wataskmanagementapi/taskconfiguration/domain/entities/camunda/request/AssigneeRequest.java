package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@JsonNaming
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
