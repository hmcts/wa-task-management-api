package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

@Component
@Profile({"integration","replica"})
public class IntegrationTestUtils {

    public static final int MAX_ROLE_ASSIGNMENT_RECORDS = 50;

    @Autowired
    protected Jackson2ObjectMapperBuilder mapperBuilder;

    @Getter
    public ObjectMapper objectMapper;

    @PostConstruct
    public void setUp() {
        objectMapper = mapperBuilder.build();
    }

    public String asJsonString(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

}
