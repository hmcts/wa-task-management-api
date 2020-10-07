package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CamundaFeignConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;

import java.util.List;
import java.util.Map;

@FeignClient(
    name = "tasks",
    url = "${camunda.url}",
    configuration = CamundaFeignConfiguration.class
)
@Service
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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

    @PostMapping(
        value = "/task/{task-id}/unclaim",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    void unclaimTask(@PathVariable("task-id") String id);

    @PostMapping(
        value = "/task/{task-id}/localVariables",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    void addLocalVariables(@PathVariable("task-id") String id,
                   @RequestBody AddLocalVariableRequest addLocalVariableRequest);

    @GetMapping(value = "/history/variable-instance", produces = MediaType.APPLICATION_JSON_VALUE)
    List<HistoryVariableInstance> getTaskVariables(@RequestParam("taskIdIn") String taskId);

    @PostMapping(value = "/task/{task-id}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody void completeTask(@PathVariable("task-id") String id, CompleteTaskVariables variables);

    @PostMapping(value = "/task/{id}/localVariables", produces = MediaType.APPLICATION_JSON_VALUE)
    void addLocalVariablesToTask(@PathVariable("id") String taskId, AddLocalVariableRequest addLocalVariableRequest);

}
