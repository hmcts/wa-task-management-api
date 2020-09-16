package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.ResourceNotFoundException;

@Service
public class CamundaService {

    private final CamundaServiceApi camundaServiceApi;

    @Autowired
    public CamundaService(CamundaServiceApi camundaServiceApi) {
        this.camundaServiceApi = camundaServiceApi;
    }

    public String getTask(String id) {
        try {
            return camundaServiceApi.getTask(id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(String.format(
                "There was a problem fetching the task with id: %s",
                id
            ), ex);
        }

    }


}
