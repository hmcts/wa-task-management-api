package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameter;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

@Schema(
    name = "SearchTaskRequest",
    description = "Search task request containing a list of parameters"
)
@EqualsAndHashCode
@ToString
@SuppressWarnings("java:S1452")
public class SearchTaskRequest {

    @Schema(
        required = true,
        description = "https://tools.hmcts.net/confluence/display/WA/WA+Task+Management+API+Guidelines")
    @NotEmpty(message = "At least one search_parameter element is required.")
    private List<@Valid SearchParameter<?>> searchParameters;
    private List<SortingParameter> sortingParameters;

    private SearchTaskRequest() {
        //Default constructor for deserialization
        super();
    }

    public SearchTaskRequest(List<SearchParameter<?>> searchParameters) {
        this.searchParameters = searchParameters;
    }

    public SearchTaskRequest(List<SearchParameter<?>> searchParameters,
                             List<SortingParameter> sortingParameters) {
        this.searchParameters = searchParameters;
        this.sortingParameters = sortingParameters;
    }

    public List<SearchParameter<?>> getSearchParameters() {
        return searchParameters;
    }

    public List<SortingParameter> getSortingParameters() {
        return sortingParameters;
    }
}
