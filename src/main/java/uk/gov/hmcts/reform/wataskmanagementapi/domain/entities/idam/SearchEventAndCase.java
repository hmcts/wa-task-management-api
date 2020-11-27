package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(
    value       = "SearchEventAndCase",
    description = "Search task request with event and case id"
)
@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement"})
public class SearchEventAndCase {

    @ApiModelProperty(required = true)
    @JsonProperty(value = "case-id")
    private String caseId;
    @JsonProperty(value = "event-id")
    @ApiModelProperty(required = true)
    private String eventId;

    private SearchEventAndCase() {
        //Default constructor for deserialization
        super();
    }

    public SearchEventAndCase(String caseId, String eventId) {
        this.caseId = caseId;
        this.eventId = eventId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getEventId() {
        return eventId;
    }

}
