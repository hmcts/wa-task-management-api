package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(
    name = "SearchEventAndCase",
    description = "Search task request with event and case id"
)
@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement"})
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchEventAndCase {

    @Schema(required = true)
    private final String caseId;
    @Schema(required = true)
    private final String eventId;
    @Schema(required = true)
    private final String caseJurisdiction;
    @Schema(required = true)
    private final String caseType;

    @JsonCreator
    public SearchEventAndCase(@JsonProperty("caseId") @JsonAlias("case_id") String caseId,
                              @JsonProperty("eventId") @JsonAlias("event_id") String eventId,
                              @JsonProperty("caseJurisdiction") @JsonAlias("case_jurisdiction") String caseJurisdiction,
                              @JsonProperty("caseType") @JsonAlias("case_type") String caseType) {
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
