package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    public TerminateTaskRequest(@JsonProperty("terminateInfo") @JsonAlias("terminate_info")
                                        TerminateInfo terminateInfo) {
        this.terminateInfo = terminateInfo;
    }

    public TerminateInfo getTerminateInfo() {
        return terminateInfo;
    }
}
