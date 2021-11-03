package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

@ApiModel(
    value = "SearchTaskRequest",
    description = "Search task request containing a list of parameters"
)
@EqualsAndHashCode
@ToString
public class SearchTaskRequest {

    @ApiModelProperty(
        required = true,
        notes = "https://tools.hmcts.net/confluence/display/WA/WA+Task+Management+API+Guidelines")
    @NotEmpty(message = "At least one search_parameter element is required.")
    private List<@Valid SearchParameter> searchParameters;
    private List<SortingParameter> sortingParameters;

    private SearchTaskRequest() {
        //Default constructor for deserialization
        super();
    }

    public SearchTaskRequest(List<SearchParameter> searchParameters) {
        this.searchParameters = searchParameters;
    }

    public SearchTaskRequest(List<SearchParameter> searchParameters,
                             List<SortingParameter> sortingParameters) {
        this.searchParameters = searchParameters;
        this.sortingParameters = sortingParameters;
    }

    public List<SearchParameter> getSearchParameters() {
        return searchParameters;
    }

    public List<SortingParameter> getSortingParameters() {
        return sortingParameters;
    }
}
