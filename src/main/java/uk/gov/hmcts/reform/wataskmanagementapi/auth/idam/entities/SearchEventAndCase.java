package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private final String caseId;
    @Schema(requiredMode = REQUIRED, name = "event_id")
    private final String eventId;
    @Schema(requiredMode = REQUIRED, name = "case_jurisdiction")
    private final String caseJurisdiction;
    @Schema(requiredMode = REQUIRED, name = "case_type")
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
