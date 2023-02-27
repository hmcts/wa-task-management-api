package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CamelCaseFeignConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskTypesDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskTypesDmnResponse;

import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "tasks",
    url = "${camunda.url}",
    configuration = CamelCaseFeignConfiguration.class
)
@Service
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UnnecessaryFullyQualifiedName", "PMD.TooManyMethods"})
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
    List<CamundaTask> searchWithCriteriaAndNoPagination(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
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
        value = "/decision-definition/key/{key}/tenant-id/{jurisdiction}/evaluate",
        consumes = APPLICATION_JSON_VALUE
    )
    List<Map<String, CamundaVariable>> evaluateDMN(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                                   @PathVariable("key") String key,
                                                   @PathVariable("jurisdiction") String jurisdiction,
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


    @DeleteMapping(
        value = "/history/variable-instance/{variable-instance-id}",
        consumes = APPLICATION_JSON_VALUE
    )
    void deleteVariableFromHistory(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                   @PathVariable("variable-instance-id") String variableInstanceId);


    @PostMapping(
        value = "/history/variable-instance",
        produces = APPLICATION_JSON_VALUE,
        consumes = APPLICATION_JSON_VALUE
    )
    List<HistoryVariableInstance> searchHistory(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                                @RequestBody Map<String, Object> body);

    @PostMapping(
        value = "/decision-definition/key/{dmn-table-key}/tenant-id/{jurisdiction}/evaluate",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    List<PermissionsDmnEvaluationResponse> evaluatePermissionsDmnTable(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("dmn-table-key") String dmnTableKey,
        @PathVariable("jurisdiction") String jurisdiction,
        DmnRequest<DecisionTableRequest> evaluateDmnRequest
    );

    @PostMapping(
        value = "/decision-definition/key/{dmn-table-key}/tenant-id/{jurisdiction}/evaluate",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    List<ConfigurationDmnEvaluationResponse> evaluateConfigurationDmnTable(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("dmn-table-key") String dmnTableKey,
        @PathVariable("jurisdiction") String jurisdiction,
        DmnRequest<DecisionTableRequest> evaluateDmnRequest
    );

    @GetMapping(
        value = "/decision-definition/",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    List<TaskTypesDmnResponse> getTaskTypesDmnTable(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @RequestParam("tenantIdIn") String jurisdiction,
        @RequestParam("name") String dmnName
    );

    @GetMapping(
        value = "/decision-definition/key/{dmn-table-key}/tenant-id/{jurisdiction}/evaluate",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    List<TaskTypesDmnEvaluationResponse> evaluateTaskTypesDmnTable(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("dmn-table-key") String dmnTableKey,
        @PathVariable("jurisdiction") String jurisdiction,
        DmnRequest<DecisionTableRequest> evaluateDmnRequest
    );
}
