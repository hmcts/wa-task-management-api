package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator.MANDATORY_FIELD_CHECK_FLAG_VARIANT;

@ExtendWith(MockitoExtension.class)
class JsonParserUtilsTest {

    private JsonParserUtils jsonParserUtils;

    private ObjectMapper objectMapper;



    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jsonParserUtils = new JsonParserUtils(objectMapper);
    }

    @Test
    @DisplayName("should return null when JSON string is valid but node name is invalid")
    void given_valid_json_string_and_invalid_node_name_when_parse_json_then_return_null() {
        String jsonString = "{\"jurisdiction\":[\"WA\"]}";
        JsonNode result = jsonParserUtils.parseJson(jsonString, MANDATORY_FIELD_CHECK_FLAG_VARIANT.stringValue());
        assertNull(result);
    }

    @Test
    @DisplayName("should return null when JSON string is empty")
    void given_empty_json_string_when_parse_json_then_return_null() {
        String emptyJson = "";
        JsonNode result = jsonParserUtils.parseJson(emptyJson, MANDATORY_FIELD_CHECK_FLAG_VARIANT.stringValue());
        assertNull(result);
    }

    @Test
    @DisplayName("should return JsonNode when JSON string is valid")
    void given_valid_json_string_when_parse_json_then_return_json_node() {
        String validJson = "{\"jurisdictions\":[\"WA\"]}";
        JsonNode result = jsonParserUtils.parseJson(validJson,  MANDATORY_FIELD_CHECK_FLAG_VARIANT.stringValue());
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals("WA", result.get(0).asText());
    }

}
