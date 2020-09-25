package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CamundaErrorDecoderTest {

    CamundaErrorDecoder camundaErrorDecoder;

    @Before
    public void setUp() {
        camundaErrorDecoder = new CamundaErrorDecoder();
    }

    @Test
    public void should_decode_and_extract_message_from_camunda_exception() {

        String exception = "{\"type\": \"NullPointerException\", \"message\": \"exception message\"}";
        String result = camundaErrorDecoder.decode(exception);

        assertEquals("exception message", result);
    }

    @Test
    public void should_throw_an_unrecognized_property_exception() {

        String exception = "{\"invalid\": \"NullPointerException\", \"message\": \"exception message\"}";

        assertThatThrownBy(() -> camundaErrorDecoder.decode(exception))
            .isInstanceOf(UnrecognizedPropertyException.class)
            .hasNoCause();
    }

    @Test
    public void should_throw_a_json_parse_exception() {

        String exception = "{\"invalid\"}";

        assertThatThrownBy(() -> camundaErrorDecoder.decode(exception))
            .isInstanceOf(JsonParseException.class)
            .hasNoCause();
    }
}
