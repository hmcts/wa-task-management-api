package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.util.List;

public class DmnConfigFieldUtils {

    private DmnConfigFieldUtils() {

    }

    public static void cleanFieldsWithInternalDefaults(
        List<String> dmnConfigFieldsWithInternalDefaults,
        List<ConfigurationDmnEvaluationResponse> dmnResponse
    ) {

        /**
         * Loop through each field in dmnConfigFieldsWithInternalDefaults and check if the field is equal to the
         * name from dmnResponse. If the name is present, then get the value of the field. Check if the value is
         * null or empty for that field if yes then remove the field from the dmnResponse so that it won't override
         * existing value.
         */

        dmnConfigFieldsWithInternalDefaults.forEach(
            field ->
                dmnResponse.removeIf(response -> {
                    String nameValue = response.getName().getValue();
                    String responseValue = response.getValue() != null ? response.getValue().getValue() : null;
                    return field.equals(nameValue) && (responseValue == null || responseValue.isEmpty());
                })
        );
    }
}
