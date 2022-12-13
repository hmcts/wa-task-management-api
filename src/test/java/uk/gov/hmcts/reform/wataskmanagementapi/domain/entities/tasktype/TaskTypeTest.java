package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import pl.pojo.tester.api.assertion.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

@ExtendWith(OutputCaptureExtension.class)
class TaskTypeTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = TaskType.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }

    @Test
    void should_return_true_when_second_task_type_id_is_upper_case() {
        String taskTypeId = "processApplication";
        String taskTypeName = "process application";
        String taskTypeId2 = "PROCESSAPPLICATION";
        String taskTypeName2 = "process application";

        TaskType taskType = new TaskType(taskTypeId, taskTypeName);
        TaskType taskType2 = new TaskType(taskTypeId2, taskTypeName2);

        assertTrue(taskType.equals(taskType2));
    }

    @Test
    void should_return_true_when_second_task_type_id_is_same() {
        String taskTypeId = "processApplication";
        String taskTypeName = "process application";
        String taskTypeId2 = "processApplication";
        String taskTypeName2 = "process application";

        TaskType taskType = new TaskType(taskTypeId, taskTypeName);
        TaskType taskType2 = new TaskType(taskTypeId2, taskTypeName2);

        assertTrue(taskType.equals(taskType2));
    }

    @Test
    void should_return_false_when_second_task_type_id_is_different() {
        String taskTypeId = "processApplication";
        String taskTypeName = "process application";
        String taskTypeId2 = "processApplication2";
        String taskTypeName2 = "process application";

        TaskType taskType = new TaskType(taskTypeId, taskTypeName);
        TaskType taskType2 = new TaskType(taskTypeId2, taskTypeName2);

        assertFalse(taskType.equals(taskType2));
    }

    @Test
    void should_return_false_when_object_null() {
        String taskTypeId = "processApplication";
        String taskTypeName = "process application";

        TaskType taskType = new TaskType(taskTypeId, taskTypeName);
        TaskType taskType2 = null;

        assertFalse(taskType.equals(taskType2));
    }

    @Test
    void should_return_same_hash_code_when_second_task_type_id_is_upper_case() {
        String taskTypeId = "processApplication";
        String taskTypeName = "process application";
        String taskTypeId2 = "PROCESSAPPLICATION";
        String taskTypeName2 = "process application";

        TaskType taskType = new TaskType(taskTypeId, taskTypeName);
        TaskType taskType2 = new TaskType(taskTypeId2, taskTypeName2);

        assertEquals(-1939939952, taskType.hashCode());
        assertEquals(taskType.hashCode(), taskType2.hashCode());
    }

    @Test
    void should_return_same_hash_code_when_second_task_type_id_is_same() {
        String taskTypeId = "processApplication";
        String taskTypeName = "process application";
        String taskTypeId2 = "processApplication";
        String taskTypeName2 = "process application";

        TaskType taskType = new TaskType(taskTypeId, taskTypeName);
        TaskType taskType2 = new TaskType(taskTypeId2, taskTypeName2);

        assertEquals(taskType.hashCode(), taskType2.hashCode());
    }

    @Test
    void should_return_different_hash_code_when_second_task_type_id_is_different() {
        String taskTypeId = "processApplication";
        String taskTypeName = "process application";
        String taskTypeId2 = "process_Application";
        String taskTypeName2 = "process application";

        TaskType taskType = new TaskType(taskTypeId, taskTypeName);
        TaskType taskType2 = new TaskType(taskTypeId2, taskTypeName2);

        assertNotEquals(taskType.hashCode(), taskType2.hashCode());
    }

    @Test
    void should_write_log_when_duplicate_task_type_id_found(CapturedOutput output) {
        String taskTypeId = "processApplication";
        String taskTypeName = "process application";
        String taskTypeId2 = "PROCESSAPPLICATION";
        String taskTypeName2 = "PROCESS APPLICATION";

        TaskType taskType = new TaskType(taskTypeId, taskTypeName);
        TaskType taskType2 = new TaskType(taskTypeId2, taskTypeName2);
        taskType.equals(taskType2);
        String expectedMessage = "Duplicate task type found for.";

        Assertions.assertThat(output.getOut().contains(expectedMessage));
    }

}
