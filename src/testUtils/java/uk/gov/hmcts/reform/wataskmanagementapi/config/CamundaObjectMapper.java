package uk.gov.hmcts.reform.wataskmanagementapi.config;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
