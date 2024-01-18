package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CamundaMessageTest {

    @Test
    void has_correct_values() {
        assertEquals("createTaskMessage", CamundaMessage.CREATE_TASK_MESSAGE.toString());
    }

    @Test
    void if_this_test_fails_it_is_because_it_needs_updating_with_your_changes() {
        assertEquals(1, CamundaMessage.values().length);
    }
}
