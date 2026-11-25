package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

@Service
public class CamundaObjectMapper {

    private final ObjectMapper defaultMapper;
    private final ObjectMapper camundaMapper;

    public CamundaObjectMapper() {
        //Hide constructor
        this.defaultMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();
        this.camundaMapper = JsonMapper.builder()
            .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();
    }

    public <T> Optional<T> read(CamundaVariable variable, Class<T> type) {
        return this.map(variable, type);
    }

    public <T> Optional<T> read(CamundaVariable variable, TypeReference<T> typeReference) {
        return this.map(variable, typeReference);
    }

    public String asJsonString(final Object obj) {
        return jsonString(obj, defaultMapper);
    }

    public String asCamundaJsonString(final Object obj) {
        return jsonString(obj, camundaMapper);
    }

    public <T> T readValue(String value, Class<T> type) {
        try {
            return camundaMapper.readValue(value, type);
        } catch (JacksonException e) {
            return null;
        }
    }

    private <T> Optional<T> map(CamundaVariable variable, Class<T> type) {

        if (variable == null) {
            return Optional.empty();
        }
        T value = defaultMapper.convertValue(variable.getValue(), type);

        return Optional.of(value);
    }

    private <T> Optional<T> map(CamundaVariable variable, TypeReference<T> typeReference) {
        if (variable == null) {
            return Optional.empty();
        }
        T value = defaultMapper.convertValue(variable.getValue(), typeReference);

        return value == null ? Optional.empty() : Optional.of(value);
    }

    private String jsonString(Object obj, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
