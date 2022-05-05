package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisionTableTest {

    @Test
    void simple_enum_example_outside_class_test() {
        assertEquals("wa-task-completion", DecisionTable.WA_TASK_COMPLETION.getTableName());
        assertEquals("wa-task-configuration", DecisionTable.WA_TASK_CONFIGURATION.getTableName());
        assertEquals("wa-task-permissions", DecisionTable.WA_TASK_PERMISSIONS.getTableName());
    }


    @Test
    void getTableKey() {
        assertEquals(
            "wa-task-completion-somejurisdiction-somecasetype",
            DecisionTable.WA_TASK_COMPLETION.getTableKey("someJurisdiction", "someCaseType")
        );
        assertEquals(
            "wa-task-completion-somejurisdiction-somecasetype",
            DecisionTable.WA_TASK_COMPLETION.getTableKey("SOMEJURISDICTION", "SOMECASETYPE")
        );
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int enumLength = DecisionTable.values().length;
        assertEquals(3, enumLength);
    }
}
