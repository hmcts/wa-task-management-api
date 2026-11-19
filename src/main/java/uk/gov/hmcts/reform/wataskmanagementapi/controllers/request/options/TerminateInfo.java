package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(
    name = "TerminationInfo",
    description = "Termination additional data"
)
@EqualsAndHashCode
@ToString
public class TerminateInfo {

    @Schema(name = "terminate_reason")
    @JsonProperty("terminate_reason")
    @JsonAlias("terminateReason")
    private final String terminateReason;

    @JsonCreator
    public TerminateInfo(
        @JsonProperty("terminate_reason") @JsonAlias("terminateReason") String terminateReason
    ) {
        this.terminateReason = terminateReason;
    }

    public String getTerminateReason() {
        return terminateReason;
    }
}
