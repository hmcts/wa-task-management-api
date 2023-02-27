package uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;
import java.util.Objects;

@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CaseDetails {

    private String jurisdiction;
    private String caseType;
    private String securityClassification;
    private Map<String, Object> data;

    public CaseDetails() {
        super();
        //No-op constructor
    }

    public CaseDetails(String jurisdiction,
                       String caseType,
                       String securityClassification,
                       Map<String, Object> data) {
        Objects.requireNonNull(jurisdiction, "Case details 'jurisdiction' cannot be null");
        Objects.requireNonNull(caseType, "Case details 'caseType' cannot be null in ");
        this.jurisdiction = jurisdiction;
        this.caseType = caseType;
        this.securityClassification = securityClassification;
        this.data = data;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getCaseType() {
        return caseType;
    }

    public String getSecurityClassification() {
        return securityClassification;
    }

    public Map<String, Object> getData() {
        return data;
    }

}
