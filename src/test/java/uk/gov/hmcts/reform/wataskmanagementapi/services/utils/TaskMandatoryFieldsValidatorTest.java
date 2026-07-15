package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.ServiceMandatoryFieldValidationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class TaskMandatoryFieldsValidatorTest {

    @Mock
    private TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;

    private String taskId;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        taskMandatoryFieldsValidator = new TaskMandatoryFieldsValidator(
            List.of("caseId", "caseName", "taskId"));
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
            List.of("caseId", "caseName", "taskId", "workTypeResource"));
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
            List.of("caseId", "caseName", "taskId", "executionTypeCode"));
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
            List.of("invalidField"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(task));
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
