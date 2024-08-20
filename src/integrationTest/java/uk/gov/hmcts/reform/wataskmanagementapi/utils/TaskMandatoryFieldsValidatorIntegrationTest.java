package uk.gov.hmcts.reform.wataskmanagementapi.utils;

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
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.JsonParserUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.ValidationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TaskMandatoryFieldsValidatorIntegrationTest extends SpringBootIntegrationBaseTest {

    @Autowired
    private TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Autowired
    private LDClientInterface ldClient;

    @Autowired
    private JsonParserUtils jsonParserUtils;

    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        when(launchDarklyFeatureFlagProvider.getJsonValue(any(), any(), any(), any()))
            .thenReturn(LDValue.parse("{\"jurisdictions\":[\"WA\"]}"));
    }

    @Test
    @DisplayName("should validate successfully when all mandatory fields are present")
    void given_all_mandatory_fields_present_when_validate_then_success() {
        TaskResource task = getTaskResource(taskId);
        assertDoesNotThrow(() -> taskMandatoryFieldsValidator.validate(task));
    }

    @Test
    @DisplayName("should throw ValidationException when a mandatory field is missing")
    void given_empty_mandatory_field_when_validate_then_throw_validation_exception() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId("");
        ValidationException exception = assertThrows(ValidationException.class, ()
            -> taskMandatoryFieldsValidator.validate(task));
        assertEquals("caseId cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("should throw ValidationException when a mandatory field is missing")
    void given_null_mandatory_field_when_validate_then_throw_validation_exception() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseName(null);
        ValidationException exception = assertThrows(ValidationException.class, ()
            -> taskMandatoryFieldsValidator.validate(task));
        assertEquals("caseName cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when property value cannot be found")
    void given_invalid_property_when_validate_then_throw_illegal_argument_exception() {
        TaskResource task = getTaskResource(taskId);
        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            new LaunchDarklyFeatureFlagProvider(ldClient), true, List.of("field1", "field2"),
            jsonParserUtils);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(task));
    }

    @Test
    @DisplayName("should not validate when mandatory field check is disabled")
    void given_mandatory_field_check_disabled_when_validate_then_no_validation() {
        TaskResource task = getTaskResource(taskId);
        TaskMandatoryFieldsValidator validator =
            new TaskMandatoryFieldsValidator(null, false,
                                             List.of("field1", "field2"), jsonParserUtils);
        assertDoesNotThrow(() -> validator.validate(task));
    }

    @Test
    @DisplayName("should not validate when jurisdiction is excluded")
    void given_jurisdiction_excluded_then_no_validation() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId(null);
        task.setJurisdiction("WA");

        TaskMandatoryFieldsValidator spyValidator = Mockito.spy(taskMandatoryFieldsValidator);
        spyValidator.validate(task);

        Mockito.verify(spyValidator, Mockito.never()).validateTaskMandatoryFields(task);
    }

    @Test
    @DisplayName("should throw ValidationException when mandatory field is missing")
    void given_task_with_missing_mandatory_field_when_validate_task_fields_then_throw_validation_exception() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId(null);
        ValidationException exception = assertThrows(ValidationException.class, ()
            -> taskMandatoryFieldsValidator.validateTaskMandatoryFields(task));
        assertEquals("caseId cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when property value cannot be found")
    void given_task_with_invalid_property_when_validate_task_fields_then_throw_illegal_argument_exception() {
        TaskResource task = getTaskResource(taskId);
        List<String> invalidFields = List.of("invalidField");
        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, true, invalidFields, jsonParserUtils);
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
