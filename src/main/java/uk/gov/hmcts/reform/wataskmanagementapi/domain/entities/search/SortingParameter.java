package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ApiModel(
    value = "SortingParameter",
    description = "Sorting parameter containing the field to sort on and the order"
)
@EqualsAndHashCode
@ToString
public class SortingParameter {

    @ApiModelProperty(required = true,
        allowableValues = "dueDate, due_date, taskTitle, task_title, locationName, location_name, caseCategory, "
                          + "case_category, caseId, case_id, caseName, case_name, work_type",
        notes = "Support snake_case and camelCase values",
        example = "due_date")
    private SortField sortBy;
    @ApiModelProperty(required = true, allowableValues = "asc, desc", example = "asc")
    private SortOrder sortOrder;

    private SortingParameter() {
        //Default constructor for deserialization
        super();
    }

    public SortingParameter(SortField sortBy, SortOrder sortOrder) {
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
    }

    public SortField getSortBy() {
        return sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }
}
