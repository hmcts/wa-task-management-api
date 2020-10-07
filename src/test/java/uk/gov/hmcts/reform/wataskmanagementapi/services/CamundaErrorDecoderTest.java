package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
class CamundaErrorDecoderTest {

    CamundaErrorDecoder camundaErrorDecoder;

    @BeforeEach
    public void setUp() {
        camundaErrorDecoder = new CamundaErrorDecoder();
    }

    @Test
    void should_decode_and_extract_message_from_camunda_exception() {

        String exception = "{\"type\": \"NullPointerException\", \"message\": \"exception message\"}";
        String result = camundaErrorDecoder.decode(exception);

        assertEquals("exception message", result);
    }

    @Test
    void should_throw_an_unrecognized_property_exception() {

        String exception = "{\"invalid\": \"NullPointerException\", \"message\": \"exception message\"}";

        assertThatThrownBy(() -> camundaErrorDecoder.decode(exception))
            .isInstanceOf(IllegalArgumentException.class)
            .hasRootCauseInstanceOf(UnrecognizedPropertyException.class);
    }

    @Test
    void should_throw_a_json_parse_exception() {

        String exception = "{\"invalid\"}";

        assertThatThrownBy(() -> camundaErrorDecoder.decode(exception))
            .isInstanceOf(IllegalArgumentException.class)
            .hasRootCauseInstanceOf(JsonParseException.class);
    }
}
