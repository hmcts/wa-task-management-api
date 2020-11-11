package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.Task;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GetTaskResponseTest {

    @Mock
    private Task task;

    @Test
    void should_create_object_and_get_value() {

        assertThat(task).isNotNull();
        final GetTaskResponse<Task> camundaTaskGetTaskResponse =
            new GetTaskResponse<>(task);
        assertThat(camundaTaskGetTaskResponse.getTask()).isEqualTo(task);

    }

}
