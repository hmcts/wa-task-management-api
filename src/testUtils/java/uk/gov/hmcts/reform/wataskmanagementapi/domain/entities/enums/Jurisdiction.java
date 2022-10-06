package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums;

public enum Jurisdiction {
    IA("Asylum"), WA("WaCaseType");

    private String caseType;

    Jurisdiction(String caseType) {
        this.caseType = caseType;
    }
}
