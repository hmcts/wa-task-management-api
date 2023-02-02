package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameter;

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
    @Schema(allowableValues = "ALL_WORK, AVAILABLE_TASKS", example = "ALL_WORK")
    private RequestContext requestContext;

    private SearchTaskRequest() {
        //Default constructor for deserialization
        super();
    }

    public SearchTaskRequest(List<SearchParameter<?>> searchParameters) {
        this.searchParameters = searchParameters;
    }

    public SearchTaskRequest(RequestContext requestContext,
                             List<SearchParameter<?>> searchParameters) {
        this.searchParameters = searchParameters;
        this.requestContext = requestContext;
    }

    public SearchTaskRequest(List<SearchParameter<?>> searchParameters,
                             List<SortingParameter> sortingParameters) {
        this.searchParameters = searchParameters;
        this.sortingParameters = sortingParameters;
    }

    public SearchTaskRequest(RequestContext requestContext,
                             List<SearchParameter<?>> searchParameters,
                             List<SortingParameter> sortingParameters) {
        this.searchParameters = searchParameters;
        this.sortingParameters = sortingParameters;
        this.requestContext = requestContext;
    }

    public List<SearchParameter<?>> getSearchParameters() {
        return searchParameters;
    }

    public List<SortingParameter> getSortingParameters() {
        return sortingParameters;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }
}
