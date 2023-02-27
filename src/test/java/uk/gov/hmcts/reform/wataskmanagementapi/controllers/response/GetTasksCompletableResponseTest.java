package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

@ExtendWith(MockitoExtension.class)
public class GetTasksCompletableResponseTest {

    @Mock
    private Task camundaTask;

    @Test
    void should_create_object_and_get_value() {

        List<Task> camundaTasks = Lists.newArrayList(camundaTask);

        final GetTasksCompletableResponse<Task> camundaTasksGetTaskResponse =
            new GetTasksCompletableResponse<>(false, camundaTasks);

        assertThat(camundaTasksGetTaskResponse.getTasks().size()).isEqualTo(1);
        assertThat(camundaTasksGetTaskResponse.getTasks().get(0)).isEqualTo(camundaTask);

    }

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = GetTasksCompletableResponse.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }
}
