package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.SearchParameters;

import java.util.List;

public class SearchTaskRequest {

    private List<SearchParameters> searchParameters;

    public SearchTaskRequest() {
        //Default constructor for deserialization
        super();
    }

    public SearchTaskRequest(List<SearchParameters> searchParameters) {
        this.searchParameters = searchParameters;
    }

    public List<SearchParameters> getSearchParameters() {
        return searchParameters;
    }

}
