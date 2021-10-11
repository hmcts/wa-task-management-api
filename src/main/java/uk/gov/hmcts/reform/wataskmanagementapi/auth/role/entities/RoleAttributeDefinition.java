package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

public enum RoleAttributeDefinition {

    CASE_ID("caseId"),
    JURISDICTION("jurisdiction"),
    PRIMARY_LOCATION("primaryLocation"),
    BASE_LOCATION("baseLocation"),
    CASE_TYPE("caseType"),
    REGION("region"),
    WORK_TYPE("workType"),
    WORK_TYPES("workTypes");

    private final String value;

    RoleAttributeDefinition(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
