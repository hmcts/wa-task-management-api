package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.ValidationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TaskMandatoryFieldsValidatorIntegrationTest extends SpringBootIntegrationBaseTest {

    @Autowired
    private TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Autowired
    private LDClientInterface ldClient;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        when(launchDarklyFeatureFlagProvider.getJsonValue(any(), any(), any(), any()))
            .thenReturn(LDValue.parse("{\"jurisdictions\":[\"WA\"]}"));
    }

    @Test
    @DisplayName("should validate successfully when all mandatory fields are present")
    void given_allMandatoryFieldsPresent_when_validate_then_success() {
        TaskResource task = getTaskResource(taskId);
        assertDoesNotThrow(() -> taskMandatoryFieldsValidator.validate(task));
    }

    @Test
    @DisplayName("should throw ValidationException when a mandatory field is missing")
    void given_emptyMandatoryField_when_validate_then_throwValidationException() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId("");
        ValidationException exception = assertThrows(ValidationException.class, ()
            -> taskMandatoryFieldsValidator.validate(task));
        assertEquals("caseId cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("should throw ValidationException when a mandatory field is missing")
    void given_NullMandatoryField_when_validate_then_throwValidationException() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseName(null);
        ValidationException exception = assertThrows(ValidationException.class, ()
            -> taskMandatoryFieldsValidator.validate(task));
        assertEquals("caseName cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when property value cannot be found")
    void given_invalidProperty_when_validate_then_throwIllegalArgumentException() {
        TaskResource task = getTaskResource(taskId);
        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            new LaunchDarklyFeatureFlagProvider(ldClient), true, List.of("field1", "field2"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(task));
    }

    @Test
    @DisplayName("should not validate when mandatory field check is disabled")
    void given_mandatoryFieldCheckDisabled_when_validate_then_noValidation() {
        TaskResource task = getTaskResource(taskId);
        TaskMandatoryFieldsValidator validator =
            new TaskMandatoryFieldsValidator(null, false, List.of("field1", "field2"));
        assertDoesNotThrow(() -> validator.validate(task));
    }

    @Test
    @DisplayName("should not validate when jurisdiction is excluded")
    void given_jurisdiction_excluded_then_noValidation() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId(null);
        task.setJurisdiction("WA");

        TaskMandatoryFieldsValidator spyValidator = Mockito.spy(taskMandatoryFieldsValidator);
        spyValidator.validate(task);

        Mockito.verify(spyValidator, Mockito.never()).validateTaskMandatoryFields(task);
    }

    @Test
    @DisplayName("should return null when JSON string is empty")
    void given_emptyJsonString_when_parseJson_then_returnNull() {
        String emptyJson = "";
        JsonNode result = taskMandatoryFieldsValidator.parseJson(emptyJson);
        assertNull(result);
    }

    @Test
    @DisplayName("should return JsonNode when JSON string is valid")
    void given_validJsonString_when_parseJson_then_returnJsonNode() {
        String validJson = "{\"jurisdictions\":[\"WA\"]}";
        JsonNode result = taskMandatoryFieldsValidator.parseJson(validJson);
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals("WA", result.get(0).asText());
    }

    @Test
    @DisplayName("should throw ValidationException when mandatory field is missing")
    void given_taskWithMissingMandatoryField_when_validateTaskFields_then_throwValidationException() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId(null);
        ValidationException exception = assertThrows(ValidationException.class, ()
            -> taskMandatoryFieldsValidator.validateTaskMandatoryFields(task));
        assertEquals("caseId cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when property value cannot be found")
    void given_taskWithInvalidProperty_when_validateTaskFields_then_throwIllegalArgumentException() {
        TaskResource task = getTaskResource(taskId);
        List<String> invalidFields = List.of("invalidField");
        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, true, invalidFields);
        assertThrows(IllegalArgumentException.class, () -> validator.validateTaskMandatoryFields(task));
    }

    private static TaskResource getTaskResource(String taskId) {
        final TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNASSIGNED
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction("IA");
        taskResource.setCaseTypeId("Asylum");
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId("CASE_ID");
        taskResource.setTitle("title");
        taskResource.setMajorPriority(2000);
        taskResource.setMinorPriority(500);
        taskResource.setExecutionTypeCode(
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"));
        taskResource.setCaseCategory("CaseCategory");
        taskResource.setCaseName("CaseName");
        return taskResource;
    }
}
