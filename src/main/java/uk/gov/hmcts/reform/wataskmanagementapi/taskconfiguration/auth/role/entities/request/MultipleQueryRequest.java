package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
