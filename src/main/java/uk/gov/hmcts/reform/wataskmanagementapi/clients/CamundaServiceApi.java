package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTaskCount;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DecisionTableResult;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DmnRequest;

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

    @PostMapping(value = "/variable-instance",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    List<CamundaVariableInstance> getAllVariables(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                                  @RequestBody Map<String, Object> body);

    @PostMapping(value = "/task",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    List<CamundaTask> searchWithCriteria(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                         @RequestBody Map<String, Object> body);

    @PostMapping(value = "/task",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    List<CamundaTask> searchWithCriteriaAndPagination(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                                      @RequestParam("firstResult") Integer firstResult,
                                                      @RequestParam("maxResults") Integer maxResults,
                                                      @RequestBody Map<String, Object> body);

    @PostMapping(value = "/task/count",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    CamundaTaskCount getTaskCount(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
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
        value = "/decision-definition/key/{key}/tenant-id/ia/evaluate",
        consumes = APPLICATION_JSON_VALUE
    )
    List<Map<String, CamundaVariable>> evaluateDMN(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                                   @PathVariable("key") String key,
                                                   @RequestBody Map<String, Map<String, CamundaVariable>> body);


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


    @PostMapping(
        value = "/decision-definition/key/{decisionTableKey}/tenant-id/ia/evaluate",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    List<DecisionTableResult> evaluateDmnTable(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("decisionTableKey") String decisionTableKey,
        DmnRequest<DecisionTableRequest> requestParameters
    );


}
