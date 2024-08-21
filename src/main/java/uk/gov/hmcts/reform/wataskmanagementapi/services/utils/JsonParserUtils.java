package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Utility class for JSON parsing.
 */
@Component
public class JsonParserUtils {

    protected final ObjectMapper objectMapper;

    /**
     * Constructor for JsonParserUtils.
     *
     * @param objectMapper the ObjectMapper to be used for JSON parsing
     */
    public JsonParserUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses a JSON string and returns the JsonNode for the specified node name.
     *
     * @param jsonString the JSON string to be parsed
     * @param nodeName the name of the node to retrieve
     * @return the JsonNode for the specified node name
     * @throws IllegalArgumentException if the JSON string cannot be parsed
     */
    public JsonNode parseJson(String jsonString, String nodeName) {
        try {
            return objectMapper.readTree(jsonString).get(nodeName);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error parsing JSON for node: " + nodeName, e);
        }
    }
}
