package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchParameterKeyTest {

    @Test
    void simple_enum_example_outside_class_test() {
        final String locationEnum = SearchParameterKey.LOCATION.value();
        final String userEnum = SearchParameterKey.USER.value();
        final String jurisdictionEnum = SearchParameterKey.JURISDICTION.value();
        final String stateEnum = SearchParameterKey.STATE.value();
        final String taskIdEnum = SearchParameterKey.TASK_ID.value();
        final String taskTypeEnum = SearchParameterKey.TASK_TYPE.value();
        final String caseIdEnum = SearchParameterKey.CASE_ID.value();
        final String workTypeEnum = SearchParameterKey.WORK_TYPE.value();
        final String availableTasksOnlyEnum = SearchParameterKey.AVAILABLE_TASKS_ONLY.value();

        assertEquals("location", locationEnum);
        assertEquals("user", userEnum);
        assertEquals("jurisdiction", jurisdictionEnum);
        assertEquals("state", stateEnum);
        assertEquals("taskId", taskIdEnum);
        assertEquals("taskType", taskTypeEnum);
        assertEquals("caseId", caseIdEnum);
        assertEquals("available_tasks_only", availableTasksOnlyEnum);
        assertEquals("work_type", workTypeEnum);
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int assigneeEnumLength = SearchParameterKey.values().length;
        assertEquals(9, assigneeEnumLength);
    }


    @ParameterizedTest
    @CsvSource(
        value = {
            "LOCATION, location",
            "USER, user",
            "JURISDICTION, jurisdiction",
            "STATE, state",
            "TASK_ID, taskId",
            "TASK_TYPE, taskType",
            "CASE_ID, caseId",
            "WORK_TYPE, work_type"
        }
    )
    public void should_return_id_when_toString_method_is_called(String input, String expected) {

        assertEquals(expected, SearchParameterKey.valueOf(input).toString());

    }

    @ParameterizedTest
    @CsvSource(
        value = {
            "''",
            "' '",
            "123",
            "null",
            "some-value"
        }
    )
    public void should_throw_exception_when_input_is_invalid(String input) {

        assertThatThrownBy(() -> SearchParameterKey.valueOf(input))
            .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    public void should_throw_exception_when_input_is_null() {

        assertThatThrownBy(() -> SearchParameterKey.valueOf(null))
            .isInstanceOf(NullPointerException.class);

    }
}
