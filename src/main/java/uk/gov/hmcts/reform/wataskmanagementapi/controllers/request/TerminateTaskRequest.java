package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;

@Schema(
    name = "TerminateTaskRequest",
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
