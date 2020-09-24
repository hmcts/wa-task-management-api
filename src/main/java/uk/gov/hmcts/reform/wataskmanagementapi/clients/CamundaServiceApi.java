package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;

import java.util.Map;

@FeignClient(
    name = "tasks",
    url = "${camunda.url}"
)
@Service
public interface CamundaServiceApi {

    @GetMapping(
        value = "/task/{task-id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    CamundaTask getTask(@PathVariable("task-id") String id);

    @PostMapping(
        value = "/task/{task-id}/claim",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    void claimTask(@PathVariable("task-id") String id,
                   @RequestBody Map<String, String> body);

}
