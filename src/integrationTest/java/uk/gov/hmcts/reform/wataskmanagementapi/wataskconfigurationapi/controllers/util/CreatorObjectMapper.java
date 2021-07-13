package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.controllers.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

public class CreatorObjectMapper {
    private CreatorObjectMapper() {
    }

    public static String asJsonString(final Object obj) {
        try {
            final ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
