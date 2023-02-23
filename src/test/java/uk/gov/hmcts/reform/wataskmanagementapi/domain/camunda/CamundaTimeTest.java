package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class CamundaTimeTest {

    @Test
    void should_get_camunda_time_format() {
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSSZ", CamundaTime.CAMUNDA_DATA_TIME_FORMAT);
    }

}
