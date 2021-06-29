package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ApiModel(
    value = "Warning",
    description = "Warning object containing the field to sort on and the order"
)
@EqualsAndHashCode
@ToString
public class Warning {
    @ApiModelProperty(
        notes = "A code that distinguishes which Warning is to be applied ")
    private final String warningCode;

    @ApiModelProperty(
        notes = "Text associated to the warning code that can be shown in the UI")
    private final String warningText;

    @JsonCreator
    public Warning(@JsonProperty("warningCode") String warningCode, @JsonProperty("warningText") String warningText) {
        this.warningCode = warningCode;
        this.warningText = warningText;
    }

    public String getWarningCode() {
        return warningCode;
    }

    public String getWarningText() {
        return warningText;
    }

}
