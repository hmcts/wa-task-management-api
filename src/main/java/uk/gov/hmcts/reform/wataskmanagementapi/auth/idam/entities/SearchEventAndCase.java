package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(
    name = "SearchEventAndCase",
    description = "Search task request with event and case id"
)
@SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement"})
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchEventAndCase {

    @Schema(requiredMode = REQUIRED, name = "case_id")
    @JsonProperty("case_id")
    @JsonAlias("caseId")
    private final String caseId;
    @Schema(requiredMode = REQUIRED, name = "event_id")
    @JsonProperty("event_id")
    @JsonAlias("eventId")
    private final String eventId;
    @Schema(requiredMode = REQUIRED, name = "case_jurisdiction")
    @JsonProperty("case_jurisdiction")
    @JsonAlias("caseJurisdiction")
    private final String caseJurisdiction;
    @Schema(requiredMode = REQUIRED, name = "case_type")
    @JsonProperty("case_type")
    @JsonAlias("caseType")
    private final String caseType;

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
