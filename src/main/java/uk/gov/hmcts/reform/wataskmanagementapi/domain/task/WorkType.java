package uk.gov.hmcts.reform.wataskmanagementapi.domain.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@EqualsAndHashCode
@ToString
@SuppressWarnings({"PMD.LawOfDemeter"})
@Schema(allowableValues = "WorkType")
public class WorkType {

    @Schema(
        requiredMode = REQUIRED,
        description = "Identifier for the work type"
    )
    private final String id;

    @Schema(
        requiredMode = REQUIRED,
        description = "Name of the work type"
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
