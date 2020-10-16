package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaExceptionMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;

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

    public void decodeException(FeignException exception) {
        try {
            CamundaExceptionMessage camundaException = mapper.readValue(
                exception.contentUTF8(),
                CamundaExceptionMessage.class
            );
            switch (camundaException.getType()) {
                case "TaskAlreadyClaimedException":
                    throw new ConflictException(camundaException.getMessage(), exception);
                case "NullValueException":
                    throw new ResourceNotFoundException(camundaException.getMessage(), exception);
                default:
                    throw new ServerErrorException(camundaException.getMessage(), exception);
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
