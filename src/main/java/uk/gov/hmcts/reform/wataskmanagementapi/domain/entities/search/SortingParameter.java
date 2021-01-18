package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ApiModel(
    value = "SortingParameter",
    description = "Sorting parameter containing the field to sort on and the order"
)
@EqualsAndHashCode
@ToString
public class SortingParameter {

    private SortField sortBy;
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
