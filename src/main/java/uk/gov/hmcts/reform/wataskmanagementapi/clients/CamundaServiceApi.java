package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CamundaFeignConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;

import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "tasks",
    url = "${camunda.url}",
    configuration = CamundaFeignConfiguration.class
)
@Service
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UnnecessaryFullyQualifiedName"})
public interface CamundaServiceApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @PostMapping(value = "/task",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    List<CamundaTask> searchWithCriteria(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                         @RequestBody Map<String, Object> body);

    @GetMapping(
        value = "/task/{task-id}",
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    CamundaTask getTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                        @PathVariable("task-id") String id);

    @PostMapping(
        value = "/task/{task-id}/claim",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    void claimTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                   @PathVariable("task-id") String id,
                   @RequestBody Map<String, String> body);

    @PostMapping(
        value = "/task/{task-id}/unclaim",
        produces = APPLICATION_JSON_VALUE
    )
    void unclaimTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                     @PathVariable("task-id") String id);

    @GetMapping(
        value = "/history/variable-instance",
        produces = APPLICATION_JSON_VALUE
    )
    List<HistoryVariableInstance> getTaskVariables(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                                   @RequestParam("taskIdIn") String taskId);

    @PostMapping(
        value = "/task/{task-id}/complete",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    void completeTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                      @PathVariable("task-id") String id,
                      CompleteTaskVariables variables);

    @PostMapping(
        value = "/task/{id}/localVariables",
        consumes = APPLICATION_JSON_VALUE
    )
    void addLocalVariablesToTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                 @PathVariable("id") String taskId,
                                 AddLocalVariableRequest addLocalVariableRequest);

    @PostMapping(
        value = "/task/{task-id}/assignee",
        consumes = APPLICATION_JSON_VALUE
    )
    void assignTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                    @PathVariable("task-id") String id,
                    @RequestBody Map<String, String> body);


    @PostMapping(
        value = "/decision-definition/key/{key}/evaluate",
        consumes = APPLICATION_JSON_VALUE
    )
    List<Map<String, CamundaVariable>>   evaluateDMN(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                    @PathVariable("key") String key,
                    @RequestBody Map<String,Map<String,CamundaVariable>> body);


    @GetMapping(
        value = "/task/{task-id}/variables",
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    Map<String, CamundaVariable> getVariables(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                              @PathVariable("task-id") String id);

    @PostMapping(
        value = "/task/{task-id}/bpmnEscalation",
        consumes = APPLICATION_JSON_VALUE
    )
    void bpmnEscalation(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                    @PathVariable("task-id") String id,
                    @RequestBody Map<String, String> body);


}
