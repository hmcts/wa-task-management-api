package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import java.util.Locale;

import static java.lang.String.format;

public enum DecisionTable {

    WA_TASK_COMPLETION("wa-task-completion"),
    WA_TASK_CONFIGURATION("wa-task-configuration"),
    WA_TASK_PERMISSIONS("wa-task-permissions");

    private final String tableName;

    DecisionTable(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableKey(String jurisdictionId, String caseTypeId) {
        return format(
            "%s-%s-%s",
            tableName,
            jurisdictionId.toLowerCase(Locale.ENGLISH),
            caseTypeId.toLowerCase(Locale.ENGLISH)
        );
    }

}
