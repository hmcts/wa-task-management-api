package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.launchdarkly.sdk.LDValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.ServiceMandatoryFieldValidationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.ValidationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TaskMandatoryFieldsValidatorTest {

    @Mock
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    private JsonParserUtils jsonParserUtils;
    private TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;

    private String taskId;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        String jsonString = "{\"jurisdictions\":[\"IA\"]}";
        lenient().when(launchDarklyFeatureFlagProvider.getJsonValue(any(), any()))
            .thenReturn(LDValue.parse(jsonString));
        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add("IA");
        JsonNode jsonNode = arrayNode;
        lenient().when(jsonParserUtils.parseJson(jsonString,  "jurisdictions")).thenReturn(jsonNode);
        taskMandatoryFieldsValidator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, true, List.of("caseId", "caseName", "taskId"), jsonParserUtils);
    }

    @Test
    @DisplayName("should validate successfully when all mandatory fields are present")
    void should_validate_successfully_when_all_mandatory_fields_are_present() {
        TaskResource task = getTaskResource(taskId);
        assertDoesNotThrow(() -> taskMandatoryFieldsValidator.validate(task));
    }

    @ParameterizedTest(name = "should throw exception when a mandatory field is missing")
    @CsvSource({
        "'', ''",
        ","
    })
    void should_throw_exception_when_service_specific_mandatory_field_is_missing(String caseId,
                                                                                 String workTypeResource) {
        taskMandatoryFieldsValidator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, true,
            List.of("caseId", "caseName", "taskId", "workTypeResource"), jsonParserUtils);
        TaskResource task = getTaskResource(taskId);
        task.setCaseId(caseId);
        task.setWorkTypeResource(new WorkTypeResource(workTypeResource, "workTypeDescription"));
        ServiceMandatoryFieldValidationException exception =
            assertThrows(ServiceMandatoryFieldValidationException.class,
                         () -> taskMandatoryFieldsValidator.validate(task));
        String message = exception.getMessage();
        assertTrue(message.contains("caseId cannot be null or empty"));
        assertTrue(message.contains("workTypeResource cannot be null or empty"));
    }

    @Test
    @DisplayName("should throw ValidationException when a tm specific mandatory field is missing")
    void should_throw_validation_exception_when_a_tm_specific_mandatory_field_is_missing() {
        TaskResource task = getTaskResource(taskId);
        taskMandatoryFieldsValidator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, true,
            List.of("caseId", "caseName", "taskId", "executionTypeCode"), jsonParserUtils);
        task.setTaskId(null);
        task.setExecutionTypeCode(new ExecutionTypeResource(null, "Manual", "Manual Description"));
        ValidationException exception =
            assertThrows(ValidationException.class,
                         () -> taskMandatoryFieldsValidator.validate(task));
        String message = exception.getMessage();
        assertTrue(message.contains("taskId cannot be null or empty"));
        assertTrue(message.contains("executionTypeCode cannot be null or empty"));

    }

    @Test
    @DisplayName("should throw ServiceMandatoryFieldValidationException when multiple mandatory fields are missing")
    void should_throw_service_mandatory_field_validation_exception_when_multiple_mandatory_fields_are_missing() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseName(null);
        task.setCaseId("");
        ServiceMandatoryFieldValidationException exception =
            assertThrows(ServiceMandatoryFieldValidationException.class,
                         () -> taskMandatoryFieldsValidator.validate(task));
        String message = exception.getMessage();
        assertTrue(message.contains("caseId cannot be null or empty"));
        assertTrue(message.contains("caseName cannot be null or empty"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when property value cannot be found")
    void should_throw_illegal_argument_exception_when_property_value_cannot_be_found() {
        TaskResource task = getTaskResource(taskId);
        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, true,
            List.of("invalidField"), jsonParserUtils);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(task));
    }

    @Test
    @DisplayName("should not validate when mandatory field check is disabled")
    void should_not_validate_when_mandatory_field_check_is_disabled() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId(null);
        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, false,
            List.of("caseId", "caseName"), jsonParserUtils);
        assertDoesNotThrow(() -> validator.validate(task));
    }

    @Test
    @DisplayName("should not validate when mandatory field check enabled services json returns null")
    void should_not_validate_when_mandatory_field_check_enabled_services_json_returns_null() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId("");
        lenient().when(launchDarklyFeatureFlagProvider.getJsonValue(any(), any()))
            .thenReturn(null);
        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, true,
            List.of("caseId", "caseName", "taskId"), jsonParserUtils);
        assertDoesNotThrow(() -> validator.validate(task));
    }

    @Test
    @DisplayName("should not validate when jurisdiction is excluded")
    void should_not_validate_when_jurisdiction_is_excluded() {
        TaskResource task = getTaskResource(taskId);
        task.setJurisdiction("IA");
        task.setCaseId("");
        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, true,
            List.of("caseId", "caseName"), jsonParserUtils);
        assertDoesNotThrow(() -> validator.validate(task));
    }

    @Test
    @DisplayName("should validate when excluded jurisdiction array is null")
    void should_validate_when_excluded_jurisdiction_array_is_null() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId("");
        lenient().when(jsonParserUtils.parseJson(Mockito.anyString(),  Mockito.anyString())).thenReturn(null);

        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, true,
            List.of("caseId", "caseName"), jsonParserUtils);
        ServiceMandatoryFieldValidationException exception =
            assertThrows(ServiceMandatoryFieldValidationException.class,
                         () -> validator.validate(task));
        String message = exception.getMessage();
        assertTrue(message.contains("caseId cannot be null or empty"));
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
        taskResource.setJurisdiction("WA");
        taskResource.setCaseTypeId("WACaseType");
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("234567");
        taskResource.setLocationName("TestLocationName");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId("CASE_ID");
        taskResource.setTitle("title");
        taskResource.setMajorPriority(2000);
        taskResource.setMinorPriority(500);
        taskResource.setExecutionTypeCode(
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"));
        taskResource.setCaseCategory("TestCategory");
        taskResource.setCaseName("TestName");
        return taskResource;
    }
}
