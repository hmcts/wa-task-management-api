package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;

@ApiModel(
    value = "TerminateTaskRequest",
    description = "Allows specifying certain termination options"
)
@EqualsAndHashCode
@ToString
public class TerminateTaskRequest {

    private final TerminateInfo terminateInfo;

    @JsonCreator
    public TerminateTaskRequest(TerminateInfo terminateInfo) {
        this.terminateInfo = terminateInfo;
    }

    public TerminateInfo getTerminateInfo() {
        return terminateInfo;
    }
}
