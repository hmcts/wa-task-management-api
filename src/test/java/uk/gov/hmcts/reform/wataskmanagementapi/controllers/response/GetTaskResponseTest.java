package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;

import static org.assertj.core.api.Assertions.assertThat;
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
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }


}
