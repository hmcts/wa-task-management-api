package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import io.swagger.annotations.ApiModel;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;

import java.util.List;

@ApiModel(
    value       = "SearchTaskRequest",
    description = "Search task request containing a list of parameters"
)
public class SearchTaskRequest {

    private List<SearchParameter> searchParameters;

    private SearchTaskRequest() {
        //Default constructor for deserialization
        super();
    }

    public SearchTaskRequest(List<SearchParameter> searchParameters) {
        this.searchParameters = searchParameters;
    }

    public List<SearchParameter> getSearchParameters() {
        return searchParameters;
    }
}
