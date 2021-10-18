package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TerminateReason;

@ApiModel(
    value = "TerminationInfo",
    description = "Termination additional data"
)
@EqualsAndHashCode
@ToString
public class TerminateInfo {

    private final TerminateReason terminateReason;

    @JsonCreator
    public TerminateInfo(@JsonProperty("terminateReason") TerminateReason terminateReason) {
        this.terminateReason = terminateReason;
    }

    public TerminateReason getTerminateReason() {
        return terminateReason;
    }
}
