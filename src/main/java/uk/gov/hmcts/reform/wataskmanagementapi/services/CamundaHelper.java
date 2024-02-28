package uk.gov.hmcts.reform.wataskmanagementapi.services;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings({
    "PMD.DataflowAnomalyAnalysis",
    "PMD.AvoidInstantiatingObjectsInLoops"
})
public final class CamundaHelper {

    private CamundaHelper() {
        //Default constructor
    }

    public static Map<String, CamundaVariable> removeSpaces(Map<String, CamundaVariable> dmnResponse) {

        HashMap<String, CamundaVariable> response = new HashMap<>(dmnResponse);

        for (HashMap.Entry<String, CamundaVariable> entry : response.entrySet()) {
            String value = entry.getValue().getValue().toString();
            if (value.contains(",") && value.contains(" ")) {
                String[] valueArray = ((String) entry.getValue().getValue()).split(",");

                List<String> trimmedValues = Arrays.stream(valueArray)
                    .map(String::trim)
                    .collect(Collectors.toList());

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

    public static PermissionsDmnEvaluationResponse removeSpaces(PermissionsDmnEvaluationResponse dmnResponse) {
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

    public static ConfigurationDmnEvaluationResponse removeSpaces(ConfigurationDmnEvaluationResponse dmnResponse) {
        ConfigurationDmnEvaluationResponse response = new ConfigurationDmnEvaluationResponse();
        response.setName(checkAndSetStringField(dmnResponse.getName()));
        response.setValue(checkAndSetStringField(dmnResponse.getValue()));
        response.setCanReconfigure(dmnResponse.getCanReconfigure());
        return response;
    }

    private static CamundaValue<String> checkAndSetStringField(CamundaValue<String> field) {
        if (field == null || field.getValue() == null) {
            return field;
        }

        return trim(field.getValue(), field.getType());
    }

    private static boolean hasSpaces(String value) {
        return value.contains(" ") && value.contains(",");
    }

    private static CamundaValue<String> trim(String value, String type) {

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
