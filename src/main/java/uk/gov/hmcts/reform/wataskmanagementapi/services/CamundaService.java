package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.ServerErrorException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@SuppressWarnings("PMD.LawOfDemeter")
public class CamundaService {

    private final CamundaServiceApi camundaServiceApi;
    private final CamundaErrorDecoder camundaErrorDecoder;

    @Autowired
    public CamundaService(CamundaServiceApi camundaServiceApi, CamundaErrorDecoder camundaErrorDecoder) {
        this.camundaServiceApi = camundaServiceApi;
        this.camundaErrorDecoder = camundaErrorDecoder;
    }

    public CamundaTask getTask(String id) {
        try {
            return camundaServiceApi.getTask(id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(String.format(
                "There was a problem fetching the task with id: %s",
                id
            ), ex);
        }

    }

    public void claimTask(String taskId, String subjectId) {
        try {
            Map<String, String> body = new ConcurrentHashMap<>();
            body.put("userId", subjectId);

            camundaServiceApi.claimTask(taskId, body);
        } catch (FeignException ex) {
            if (HttpStatus.NOT_FOUND.value() == ex.status()) {
                throw new ResourceNotFoundException(String.format(
                    "There was a problem claiming the task with id: %s",
                    taskId
                ), ex);
            } else {
                String message = camundaErrorDecoder.decode(ex.contentUTF8());
                throw new ServerErrorException(String.format(
                    "Could not claim the task with id: %s. %s", taskId, message
                ), ex);
            }
        }

    }
}
