package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaTaskServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.ResourceNotFoundException;

@Service
public class CamundaTaskService {

    private final CamundaTaskServiceApi camundaTaskServiceApi;

    @Autowired
    public CamundaTaskService(CamundaTaskServiceApi camundaTaskServiceApi) {
        this.camundaTaskServiceApi = camundaTaskServiceApi;
    }

    public String getTask(String id) {
        try {
            return camundaTaskServiceApi.getTask(id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException("There was a problem fetching the task with id: " + id);
        }

    }


}
