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
    @JsonProperty(value = "case-jurisdiction")
    private String caseJurisdiction;
    @JsonProperty(value = "case-type")
    private String caseType;

    private SearchEventAndCase() {
        //Default constructor for deserialization
        super();
    }

    public SearchEventAndCase(String caseId, String eventId,
                              String caseJurisdiction, String caseType) {
        this.caseId = caseId;
        this.eventId = eventId;
        this.caseJurisdiction = caseJurisdiction;
        this.caseType = caseType;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getCaseJurisdiction() {
        return caseJurisdiction;
    }

    public String getCaseType() {
        return caseType;
    }
}
