package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DmnConfigFieldUtilTest {

    @Test
    void should_remove_field_with_null_value() {
        List<ConfigurationDmnEvaluationResponse> responses = new ArrayList<>();
        ConfigurationDmnEvaluationResponse configurationDmnEvaluationResponse =
            new ConfigurationDmnEvaluationResponse();
        configurationDmnEvaluationResponse.setName(new CamundaValue<>("field1", "String"));
        configurationDmnEvaluationResponse.setValue(null);
        responses.add(configurationDmnEvaluationResponse);
        List<String> fields = List.of("field1");

        DmnConfigFieldUtils.cleanFieldsWithInternalDefaults(fields, responses);

        assertTrue(responses.isEmpty());
    }

    @Test
    void should_remove_field_with_empty_value() {
        List<ConfigurationDmnEvaluationResponse> responses = new ArrayList<>();
        ConfigurationDmnEvaluationResponse configurationDmnEvaluationResponse =
            new ConfigurationDmnEvaluationResponse();
        configurationDmnEvaluationResponse.setName(new CamundaValue<>("field1", "String"));
        configurationDmnEvaluationResponse.setValue(new CamundaValue<>("", "String"));
        responses.add(configurationDmnEvaluationResponse);
        List<String> fields = List.of("field1");

        DmnConfigFieldUtils.cleanFieldsWithInternalDefaults(fields, responses);

        assertTrue(responses.isEmpty());
    }

    @Test
    void should_not_remove_field_with_non_empty_value() {
        List<ConfigurationDmnEvaluationResponse> responses = new ArrayList<>();
        ConfigurationDmnEvaluationResponse configurationDmnEvaluationResponse =
            new ConfigurationDmnEvaluationResponse();
        configurationDmnEvaluationResponse.setName(new CamundaValue<>("field1", "String"));
        configurationDmnEvaluationResponse.setValue(new CamundaValue<>("someValue", "String"));
        responses.add(configurationDmnEvaluationResponse);
        List<String> fields = List.of("field1");

        DmnConfigFieldUtils.cleanFieldsWithInternalDefaults(fields, responses);

        assertFalse(responses.isEmpty());
        assertEquals("someValue", responses.getFirst().getValue().getValue());
    }

}
