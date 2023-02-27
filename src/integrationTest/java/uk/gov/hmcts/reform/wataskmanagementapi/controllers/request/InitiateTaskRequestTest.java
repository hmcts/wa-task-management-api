package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;

class InitiateTaskRequestTest extends SpringBootIntegrationBaseTest {

    @Test
    void given_snake_case_initiate_body_request_when_deserializes_it_keeps_attribute_list_and_operation_fields()
        throws JsonProcessingException {
        Map<String, Object> taskAttributes = Map.of(
            TASK_TYPE.value(), "aTaskType",
            TASK_NAME.value(), "aTaskName",
            CASE_ID.value(), "1634748573864804",
            TASK_TITLE.value(), "A test task"
        );

        InitiateTaskRequestMap initiateTaskRequest = new InitiateTaskRequestMap(INITIATION, taskAttributes);

        String expectedInitiateBodyRequest = asJsonString(initiateTaskRequest);

        InitiateTaskRequestMap actual = objectMapper.readValue(
            expectedInitiateBodyRequest,
            InitiateTaskRequestMap.class
        );

        assertThat(actual.getTaskAttributes()).isNotNull();
        assertThat(actual.getTaskAttributes()).isEqualTo(taskAttributes);
        assertThat(actual.getOperation()).isEqualTo(INITIATION);
    }

}
