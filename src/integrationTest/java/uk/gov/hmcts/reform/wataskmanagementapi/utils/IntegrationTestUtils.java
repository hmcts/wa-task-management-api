package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
@Profile({"integration","replica"})
public class IntegrationTestUtils {

    public static final int MAX_ROLE_ASSIGNMENT_RECORDS = 50;

    @Autowired
    protected JsonMapper mapper;

    @Getter
    public ObjectMapper objectMapper;

    @PostConstruct
    public void setUp() {
        objectMapper = mapper;
    }

    public String asJsonString(Object object) throws JacksonException {
        return objectMapper.writeValueAsString(object);
    }

}
