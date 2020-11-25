package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CamundaObjectMapper {

    private ObjectMapper defaultObjectMapper;
    private ObjectMapper camundaObjectMapper;

    private CamundaObjectMapper() {
        //Hide constructor
    }

    public CamundaObjectMapper(ObjectMapper defaultObjectMapper, ObjectMapper camundaObjectMapper) {
        this.defaultObjectMapper = defaultObjectMapper;
        this.camundaObjectMapper = camundaObjectMapper;
    }

    public String asJsonString(final Object obj) {
        return jsonString(obj, defaultObjectMapper);
    }

    public String asCamelCasedJsonString(final Object obj) {
        return jsonString(obj, camundaObjectMapper);
    }

    private String jsonString(Object obj, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

