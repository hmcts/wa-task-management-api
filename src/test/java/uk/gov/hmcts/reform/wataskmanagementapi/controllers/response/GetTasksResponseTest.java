package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.CamundaTask;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class GetTasksResponseTest {

    @Mock
    private CamundaTask camundaTask;

    @Test
    void should_create_object_and_get_value() {

        List<CamundaTask> camundaTasks = Lists.newArrayList(camundaTask);

        final GetTasksResponse<CamundaTask> camundaTasksGetTaskResponse =
            new GetTasksResponse<>(camundaTasks);

        assertThat(camundaTasksGetTaskResponse.getTasks().size()).isEqualTo(1);
        assertThat(camundaTasksGetTaskResponse.getTasks().get(0)).isEqualTo(camundaTask);

    }
}
