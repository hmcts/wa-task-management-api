package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@SuppressWarnings({"PMD.LawOfDemeter"})
@ApiModel("WorkType")
public class WorkType {

    @ApiModelProperty(
        required = true,
        notes = "Identifier for the work type"
    )
    private final String id;

    @ApiModelProperty(
        required = true,
        notes = "Name of the work type"
    )
    private final String label;

    public WorkType(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
