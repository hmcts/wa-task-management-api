package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CamelCaseFeignConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.AssigneeRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi.SERVICE_AUTHORIZATION;

@FeignClient(
    name = "camunda",
    url = "${camunda.url}",
    configuration = CamelCaseFeignConfiguration.class
)
public interface CamundaServiceApi {

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

    @PostMapping(value = "/task/{id}/localVariables", produces = MediaType.APPLICATION_JSON_VALUE)
    void addLocalVariablesToTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                 @PathVariable("id") String taskId,
                                 AddLocalVariableRequest addLocalVariableRequest);

    @PostMapping(value = "/task/{id}/assignee", produces = MediaType.APPLICATION_JSON_VALUE)
    void assignTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                    @PathVariable("id") String taskId,
                    @RequestBody AssigneeRequest assigneeRequest);

    @GetMapping(value = "/task/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    CamundaTask getTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                        @PathVariable("id") String taskId);

    @GetMapping(value = "/task/{id}/variables", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, CamundaValue<Object>> getVariables(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("id") String processInstanceId);
}

