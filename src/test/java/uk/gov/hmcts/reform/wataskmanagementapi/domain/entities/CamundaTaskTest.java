package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class CamundaTaskTest {

    @Test
    void should_create_object_and_get_value() {
        CamundaTask camundaTask = new CamundaTask("some-id");
        Assertions.assertThat(camundaTask.getId()).isEqualTo("some-id");

    }

}
