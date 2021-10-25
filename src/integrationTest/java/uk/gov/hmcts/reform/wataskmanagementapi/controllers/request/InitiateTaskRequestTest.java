package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

class InitiateTaskRequestTest extends SpringBootIntegrationBaseTest {

    @Test
    void given_snake_case_initiate_body_request_when_deserializes_it_keeps_attribute_list_and_operation_fields()
        throws JsonProcessingException {
        String expectedInitiateBodyRequest = "{\n"
                                             + "  \"operation\": \"INITIATION\",\n"
                                             + "  \"task_attributes\": [\n"
                                             + "    {\n"
                                             + "      \"name\": \"TASK_TYPE\",\n"
                                             + "      \"value\": \"aTaskType\"\n"
                                             + "    },\n"
                                             + "    {\n"
                                             + "      \"name\": \"TASK_NAME\",\n"
                                             + "      \"value\": \"aTaskName\"\n"
                                             + "    },\n"
                                             + "    {\n"
                                             + "      \"name\": \"TASK_CASE_ID\",\n"
                                             + "      \"value\": \"1634748573864804\"\n"
                                             + "    },\n"
                                             + "    {\n"
                                             + "      \"name\": \"TASK_TITLE\",\n"
                                             + "      \"value\": \"A test task\"\n"
                                             + "    }\n"
                                             + "  ]\n"
                                             + "}";

        InitiateTaskRequest actual = objectMapper.readValue(expectedInitiateBodyRequest, InitiateTaskRequest.class);

        assertThat(actual.getTaskAttributes()).isNotNull();
        assertThat(actual.getTaskAttributes()).isEqualTo(List.of(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "1634748573864804"),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));
        assertThat(actual.getOperation()).isEqualTo(INITIATION);
    }

}