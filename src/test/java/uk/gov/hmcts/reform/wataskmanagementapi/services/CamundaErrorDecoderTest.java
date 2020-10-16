package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void should_throw_a_conflict_exception_exception() {
        FeignException mockedException = mock(FeignException.class);

        when(mockedException.contentUTF8()).thenReturn(
            "  {\n"
            + "    \"type\": \"TaskAlreadyClaimedException\",\n"
            + "    \"message\": \"Task '800ea87d-0a14-11eb-8d07-0242ac11000f' is already claimed by someone else.\"\n"
            + "  }"
        );

        assertThatThrownBy(() -> camundaErrorDecoder.decodeException(mockedException))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Task '800ea87d-0a14-11eb-8d07-0242ac11000f' is already claimed by someone else.")
            .hasRootCauseInstanceOf(FeignException.class);
    }

    @Test
    void should_throw_a_resource_not_found_exception() {
        FeignException mockedException = mock(FeignException.class);

        when(mockedException.contentUTF8()).thenReturn(
            "  {\n"
            + "    \"type\": \"NullValueException\",\n"
            + "    \"message\": \"Task not found.\"\n"
            + "  }"
        );

        assertThatThrownBy(() -> camundaErrorDecoder.decodeException(mockedException))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Task not found.")
            .hasRootCauseInstanceOf(FeignException.class);
    }

    @Test
    void should_throw_a_server_error_exception() {
        FeignException mockedException = mock(FeignException.class);

        when(mockedException.contentUTF8()).thenReturn(
            "  {\n"
            + "    \"type\": \"OtherUnmappedException\",\n"
            + "    \"message\": \"Some error message.\"\n"
            + "  }"
        );

        assertThatThrownBy(() -> camundaErrorDecoder.decodeException(mockedException))
            .isInstanceOf(ServerErrorException.class)
            .hasMessage("Some error message.")
            .hasRootCauseInstanceOf(FeignException.class);
    }
}
