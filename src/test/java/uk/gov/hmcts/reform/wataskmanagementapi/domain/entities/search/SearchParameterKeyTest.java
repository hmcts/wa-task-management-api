package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchParameterKeyTest {


    @Test
    void simpleEnumExampleOutsideClassTest() {
        final String locationEnum = SearchParameterKey.LOCATION.value();
        final String userEnum = SearchParameterKey.USER.value();
        final String jurisdictionEnum = SearchParameterKey.JURISDICTION.value();
        final String stateEnum = SearchParameterKey.STATE.value();
        final String taskIdEnum = SearchParameterKey.TASK_ID.value();
        final String taskTypeEnum = SearchParameterKey.TASK_TYPE.value();
        final String caseIdEnum = SearchParameterKey.CASE_ID.value();
        final String workTypeEnum = SearchParameterKey.WORK_TYPE.value();

        assertEquals("location", locationEnum);
        assertEquals("user", userEnum);
        assertEquals("jurisdiction", jurisdictionEnum);
        assertEquals("state", stateEnum);
        assertEquals("taskId", taskIdEnum);
        assertEquals("taskType", taskTypeEnum);
        assertEquals("caseId", caseIdEnum);
        assertEquals("workType", workTypeEnum);

    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int assigneeEnumLength = SearchParameterKey.values().length;
        assertEquals(8, assigneeEnumLength);
    }
}
