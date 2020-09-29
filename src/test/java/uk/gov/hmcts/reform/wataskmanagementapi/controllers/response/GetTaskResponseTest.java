package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.CamundaTask;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class GetTaskResponseTest {

    @Mock
    private CamundaTask camundaTask;

    @Test
    void should_create_object_and_get_value() {

        assertThat(camundaTask).isNotNull();
        final GetTaskResponse<CamundaTask> camundaTaskGetTaskResponse =
            new GetTaskResponse<>(camundaTask);
        assertThat(camundaTaskGetTaskResponse.getTask()).isEqualTo(camundaTask);

    }

}
