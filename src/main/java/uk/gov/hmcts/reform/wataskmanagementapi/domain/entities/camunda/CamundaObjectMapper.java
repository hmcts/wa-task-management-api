package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CamundaObjectMapper {

    private final ObjectMapper defaultMapper;
    private final ObjectMapper camundaMapper;

    public CamundaObjectMapper() {
        //Hide constructor
        this.defaultMapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        this.camundaMapper = new ObjectMapper();
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
        } catch (JsonProcessingException e) {
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

    private String jsonString(Object obj, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> map(CamundaVariable variable, TypeReference<T> typeReference) {
        T value = defaultMapper.convertValue(variable, typeReference);

        return value == null ? Optional.empty() : Optional.of(value);
    }
}

