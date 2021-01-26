package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.List;

@ApiModel(
    value = "SearchTaskRequest",
    description = "Search task request containing a list of parameters"
)
@EqualsAndHashCode
@ToString
public class SearchTaskRequest {

    private List<SearchParameter> searchParameters;
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
