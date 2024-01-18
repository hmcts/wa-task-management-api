package uk.gov.hmcts.reform.wataskmanagementapi.domain.tasktype;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class TaskTypeResponseTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = TaskTypeResponse.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }

    @Test
    void deserializeTest() throws JsonProcessingException {

        String taskType = "{\"taskTypeId\":\"taskTypeId1\",\"taskTypeName\":\"taskTypeName1\"}";
        TaskTypeResponse taskTypeResponse = new TaskTypeResponse(taskType);
        assertNotNull(taskTypeResponse);
        assertEquals("taskTypeId1", taskTypeResponse.getTaskType().getTaskTypeId());
        assertEquals("taskTypeName1", taskTypeResponse.getTaskType().getTaskTypeName());
        assertEquals(taskType, taskTypeResponse.getTaskTypeAsJson());
    }

}
