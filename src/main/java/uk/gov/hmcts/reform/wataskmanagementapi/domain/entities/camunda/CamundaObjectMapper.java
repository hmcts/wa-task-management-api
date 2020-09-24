package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

public final class CamundaObjectMapper {
    private CamundaObjectMapper() {
        //No-op construcutor
    }

    public static String asJsonString(final Object obj) {
        return jsonString(obj, new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE));
    }

    public static String asCamundaJsonString(final Object obj) {
        return jsonString(obj, new ObjectMapper());
    }

    private static String jsonString(Object obj, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
