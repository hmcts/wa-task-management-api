package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.PermissionsDmnEvaluationResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({
    "PMD.DataflowAnomalyAnalysis",
    "PMD.AvoidInstantiatingObjectsInLoops"
})
public class CamundaHelper {

    public Map<String, CamundaVariable> removeSpaces(Map<String, CamundaVariable> response) {

        for (Map.Entry<String, CamundaVariable> entry : response.entrySet()) {
            String value = entry.getValue().getValue().toString();
            if (value.contains(",") && value.contains(" ")) {
                String[] valueArray = ((String) entry.getValue().getValue()).split(",");

                List<String> trimmedValues = Arrays.stream(valueArray)
                    .map(String::trim)
                    .collect(Collectors.toList());

                response.remove(entry.getKey());
                response.put(
                    entry.getKey(),
                    new CamundaVariable(
                        String.join(",", trimmedValues),
                        entry.getValue().getType()
                    )
                );
            }

        }
        return response;
    }

    public PermissionsDmnEvaluationResponse removeSpaces(PermissionsDmnEvaluationResponse dmnResponse) {
        PermissionsDmnEvaluationResponse response = new PermissionsDmnEvaluationResponse();
        response.setAuthorisations(checkAndSetStringField(dmnResponse.getAuthorisations()));
        response.setRoleCategory(checkAndSetStringField(dmnResponse.getRoleCategory()));
        response.setName(checkAndSetStringField(dmnResponse.getName()));
        response.setValue(checkAndSetStringField(dmnResponse.getValue()));
        response.setCaseAccessCategory(checkAndSetStringField(dmnResponse.getCaseAccessCategory()));
        response.setAssignmentPriority(dmnResponse.getAssignmentPriority());
        response.setAutoAssignable(dmnResponse.getAutoAssignable());
        return response;
    }

    public ConfigurationDmnEvaluationResponse removeSpaces(ConfigurationDmnEvaluationResponse dmnResponse) {
        ConfigurationDmnEvaluationResponse response = new ConfigurationDmnEvaluationResponse();
        response.setName(checkAndSetStringField(dmnResponse.getName()));
        response.setValue(checkAndSetStringField(dmnResponse.getValue()));
        response.setCanReconfigure(dmnResponse.getCanReconfigure());
        return response;
    }

    public CamundaValue<String> checkAndSetStringField(CamundaValue<String> field) {
        if (field == null || field.getValue() == null) {
            return field;
        }

        return trim(field.getValue(), field.getType());
    }

    public boolean hasSpaces(String value) {
        return value.contains(" ") && value.contains(",");
    }

    public CamundaValue<String> trim(String value, String type) {

        if (!hasSpaces(value)) {
            return new CamundaValue<>(
                value,
                type
            );
        }

        List<String> trimmedValues = Arrays.stream(value.split(","))
            .map(String::trim)
            .collect(Collectors.toList());

        return new CamundaValue<>(
            String.join(",", trimmedValues),
            type
        );
    }
}
