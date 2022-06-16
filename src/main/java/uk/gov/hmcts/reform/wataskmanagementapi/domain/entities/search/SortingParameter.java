package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(
    name = "SortingParameter",
    description = "Sorting parameter containing the field to sort on and the order"
)
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class SortingParameter {

    @Schema(required = true,
        allowableValues = "dueDate, due_date, taskTitle, task_title, locationName, location_name, caseCategory, "
                          + "case_category, caseId, case_id, caseName, case_name",
        description = "Support snake_case and camelCase values",
        example = "due_date")
    private final SortField sortBy;
    @Schema(required = true, allowableValues = "asc, desc", example = "asc")
    private final SortOrder sortOrder;

    public SortField getSortBy() {
        return sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }
}
