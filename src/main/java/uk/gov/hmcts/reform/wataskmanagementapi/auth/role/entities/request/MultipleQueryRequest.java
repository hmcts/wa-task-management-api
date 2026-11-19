package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@EqualsAndHashCode
@Builder
@JsonNaming
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class MultipleQueryRequest {
    private List<QueryRequest> queryRequests;

    public List<QueryRequest> getQueryRequests() {
        return queryRequests;
    }
}
