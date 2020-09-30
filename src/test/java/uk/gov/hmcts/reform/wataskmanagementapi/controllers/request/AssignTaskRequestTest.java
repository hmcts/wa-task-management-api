package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AssignTaskRequestTest {

    @Test
    void should_create_object_and_get_value() {

        final AssignTaskRequest assignTaskRequest = new AssignTaskRequest("some-user");
        assertThat(assignTaskRequest.getUserId()).isEqualTo("some-user");

    }

}
