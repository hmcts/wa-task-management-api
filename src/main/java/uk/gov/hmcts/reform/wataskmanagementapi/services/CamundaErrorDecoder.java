package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaExceptionMessage;

@Service
public class CamundaErrorDecoder {

    ObjectMapper mapper = new ObjectMapper();

    public String decode(String exception) {
        try {
            CamundaExceptionMessage exceptionMessage = mapper.readValue(exception, CamundaExceptionMessage.class);
            return exceptionMessage.getMessage();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
