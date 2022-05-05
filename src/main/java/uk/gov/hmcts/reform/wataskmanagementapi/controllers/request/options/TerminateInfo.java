package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    private final String terminateReason;

    @JsonCreator
    public TerminateInfo(String terminateReason) {
        this.terminateReason = terminateReason;
    }

    public String getTerminateReason() {
        return terminateReason;
    }
}
