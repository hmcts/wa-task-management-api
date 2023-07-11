package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class TaskOperationResponse {

    private Map<String, Object> responseMap;

    public TaskOperationResponse(Map<String, Object> responseMap) {
        this.responseMap = responseMap;
    }

    public Map<String, Object> getResponseMap() {
        return responseMap;
    }

}
