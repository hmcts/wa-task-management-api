package uk.gov.hmcts.reform.wataskmanagementapi.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisionTableTest {

    @Test
    void simple_enum_example_outside_class_test() {
        assertEquals("wa-task-completion", DecisionTable.WA_TASK_COMPLETION.getTableName());
        assertEquals("wa-task-configuration", DecisionTable.WA_TASK_CONFIGURATION.getTableName());
        assertEquals("wa-task-permissions", DecisionTable.WA_TASK_PERMISSIONS.getTableName());
        assertEquals("wa-task-types", DecisionTable.WA_TASK_TYPES.getTableName());
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
        assertEquals(
            "wa-task-completion-somejurisdiction-somecasetype_withunderscore",
            DecisionTable.WA_TASK_COMPLETION.getTableKey("SOMEJURISDICTION", "somecasetype_withunderscore")
        );
        assertEquals(
            "wa-task-completion-somejurisdiction-somecasetype#withhashtrick",
            DecisionTable.WA_TASK_COMPLETION.getTableKey("SOMEJURISDICTION", "somecasetype#withhashtrick")
        );

        assertEquals(
            "wa-task-completion-somejurisdiction-somecasetype.withdot",
            DecisionTable.WA_TASK_COMPLETION.getTableKey("SOMEJURISDICTION", "somecasetype.withdot")
        );

        assertEquals(
            "wa-task-completion-somejurisdiction-some&case%type$$",
            DecisionTable.WA_TASK_COMPLETION.getTableKey("SOMEJURISDICTION", "some&case%type$$")
        );
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int enumLength = DecisionTable.values().length;
        assertEquals(4, enumLength);
    }
}
