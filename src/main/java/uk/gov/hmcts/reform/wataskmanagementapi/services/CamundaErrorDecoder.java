package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaExceptionMessage;

@Service
public class CamundaErrorDecoder {

    ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    public String decode(String exception) {
        CamundaExceptionMessage exceptionMessage = mapper.readValue(exception, CamundaExceptionMessage.class);
        return exceptionMessage.getMessage();
    }

}
