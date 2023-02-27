package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TaskTypesDmnResponse implements EvaluationResponse {

    private String key;
    private String tenantId;
    private String resource;

    private TaskTypesDmnResponse() {
        //No-op constructor for deserialization
    }

    public TaskTypesDmnResponse(String key,
                                String tenantId,
                                String resource) {
        this.key = key;
        this.tenantId = tenantId;
        this.resource = resource;
    }

    public String getKey() {
        return key;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getResource() {
        return resource;
    }

}
