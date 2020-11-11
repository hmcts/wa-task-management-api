package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GetTasksResponseTest {

    @Mock
    private Task camundaTask;

    @Test
    void should_create_object_and_get_value() {

        List<Task> camundaTasks = Lists.newArrayList(camundaTask);

        final GetTasksResponse<Task> camundaTasksGetTaskResponse = new GetTasksResponse<>(camundaTasks);

        assertThat(camundaTasksGetTaskResponse.getTasks().size()).isEqualTo(1);
        assertThat(camundaTasksGetTaskResponse.getTasks().get(0)).isEqualTo(camundaTask);

    }
}
