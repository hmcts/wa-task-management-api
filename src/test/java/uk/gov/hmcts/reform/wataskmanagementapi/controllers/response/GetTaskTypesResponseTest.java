package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype.TaskType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype.TaskTypeResponse;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

@ExtendWith(MockitoExtension.class)
class GetTaskTypesResponseTest {

    @Test
    void should_create_object_and_get_value() {

        TaskType taskType = new TaskType("taskTypeId", "taskTypeName");

        TaskTypeResponse taskTypeResponse = new TaskTypeResponse(taskType);

        List<TaskTypeResponse> taskTypeResponses = List.of(taskTypeResponse);

        final GetTaskTypesResponse getTaskTypesResponse = new GetTaskTypesResponse(taskTypeResponses);

        assertThat(getTaskTypesResponse.getTaskTypeResponses().size())
            .isEqualTo(1);

        assertThat(getTaskTypesResponse.getTaskTypeResponses())
            .isEqualTo(taskTypeResponses);

        assertThat(getTaskTypesResponse.getTaskTypeResponses().get(0).getTaskType())
            .isEqualTo(taskType);

        assertThat(getTaskTypesResponse.getTaskTypeResponses().get(0).getTaskType().getTaskTypeId())
            .isEqualTo("taskTypeId");

        assertThat(getTaskTypesResponse.getTaskTypeResponses().get(0).getTaskType().getTaskTypeName())
            .isEqualTo("taskTypeName");

    }

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = GetTaskTypesResponse.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }
    
}
