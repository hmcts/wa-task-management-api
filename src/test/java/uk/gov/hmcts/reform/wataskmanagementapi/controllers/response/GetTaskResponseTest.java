package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

@ExtendWith(MockitoExtension.class)
class GetTaskResponseTest {

    @Mock
    private Task mappedTask;

    @Test
    void should_create_object_and_get_value() {

        assertThat(mappedTask).isNotNull();
        final GetTaskResponse<Task> camundaTaskGetTaskResponse =
            new GetTaskResponse<>(mappedTask);
        assertThat(camundaTaskGetTaskResponse.getTask()).isEqualTo(mappedTask);

    }

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = GetTaskResponse.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }

    @Test
    void equalMethod() {
        Task task2 = Mockito.mock(Task.class);
        GetTaskResponse<Task> getTaskResponse = new GetTaskResponse<>(mappedTask);
        GetTaskResponse<Task> getTaskResponse1 = new GetTaskResponse<>(task2);

        boolean result2 = getTaskResponse.equals(getTaskResponse1);
        assertFalse(result2);
    }
}
