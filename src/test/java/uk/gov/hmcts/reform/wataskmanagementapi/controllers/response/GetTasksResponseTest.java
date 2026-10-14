package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class GetTasksResponseTest {

    @Mock
    private Task camundaTask;

    @Test
    void should_create_object_and_get_value() {

        List<Task> camundaTasks = Lists.newArrayList(camundaTask);

        final GetTasksResponse<Task> camundaTasksGetTaskResponse = new GetTasksResponse<>(camundaTasks, 1);

        assertThat(camundaTasksGetTaskResponse.getTasks()).hasSize(1);
        assertThat(camundaTasksGetTaskResponse.getTasks().get(0)).isEqualTo(camundaTask);
        assertThat(camundaTasksGetTaskResponse.getHasMoreRecords()).isNull();

    }

    @Test
    void should_serialize_optional_has_more_records_without_changing_the_legacy_response_shape()
        throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        String responseWithMetadata = objectMapper.writeValueAsString(
            new GetTasksResponse<Task>(List.of(), 5000, true)
        );
        String legacyResponse = objectMapper.writeValueAsString(new GetTasksResponse<Task>(List.of(), 1));

        assertThat(responseWithMetadata).contains("\"has_more_records\":true");
        assertThat(legacyResponse).doesNotContain("has_more_records");
    }

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = GetTasksResponse.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }


}
