package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.MappedTask;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GetTaskResponseTest {

    @Mock
    private MappedTask mappedTask;

    @Test
    void should_create_object_and_get_value() {

        assertThat(mappedTask).isNotNull();
        final GetTaskResponse<MappedTask> camundaTaskGetTaskResponse =
            new GetTaskResponse<>(mappedTask);
        assertThat(camundaTaskGetTaskResponse.getTask()).isEqualTo(mappedTask);

    }

}
