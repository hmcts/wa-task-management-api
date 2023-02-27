package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;

import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.REGION;

@Service
public class AttributesValueVerifier {

    private final CamundaObjectMapper camundaObjectMapper;

    @Autowired
    protected AttributesValueVerifier(CamundaObjectMapper camundaObjectMapper) {
        this.camundaObjectMapper = camundaObjectMapper;
    }

    protected boolean hasJurisdictionPermission(String roleAssignmentJurisdiction,
                                                Map<String, CamundaVariable> variables) {
        String taskJurisdiction = getVariableValue(variables.get(JURISDICTION.value()), String.class);
        return roleAssignmentJurisdiction.equals(taskJurisdiction);
    }

    protected boolean hasLocationPermission(String roleAssignmentLocation, Map<String, CamundaVariable> variables) {
        String taskLocation = getVariableValue(variables.get(LOCATION.value()), String.class);
        return roleAssignmentLocation.equals(taskLocation);
    }

    protected boolean hasRegionPermission(String roleAssignmentRegion, Map<String, CamundaVariable> variables) {
        String taskRegion = getVariableValue(variables.get(REGION.value()), String.class);
        return roleAssignmentRegion.equals(taskRegion);
    }

    protected boolean hasCaseIdPermission(String roleAssignmentCaseId, Map<String, CamundaVariable> variables) {
        String taskCaseId = getVariableValue(variables.get(CASE_ID.value()), String.class);
        return roleAssignmentCaseId.equals(taskCaseId);

    }

    protected boolean hasCaseTypeIdPermission(String roleAssignmentCaseTypeId, Map<String, CamundaVariable> variables) {
        String caseTypeId = getVariableValue(variables.get(CASE_TYPE_ID.value()), String.class);
        return roleAssignmentCaseTypeId.equals(caseTypeId);
    }

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }
}
