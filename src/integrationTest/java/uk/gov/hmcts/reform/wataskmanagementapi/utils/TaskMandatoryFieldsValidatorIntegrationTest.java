package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
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
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.ServiceMandatoryFieldValidationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.JsonParserUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @Autowired
    private JsonParserUtils jsonParserUtils;

    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        when(launchDarklyFeatureFlagProvider.getJsonValue(any(), any()))
            .thenReturn(LDValue.parse("{\"jurisdictions\":[\"WA\"]}"));
    }

    @Test
    @DisplayName("should validate successfully when all mandatory fields are present")
    void should_validate_successfully_when_all_mandatory_fields_are_present() {
        TaskResource task = getTaskResource(taskId);
        assertDoesNotThrow(() -> taskMandatoryFieldsValidator.validate(task));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("should throw ServiceMandatoryFieldValidationException when a mandatory field is empty")
    void should_throw_service_mandatory_field_validation_exception_when_a_mandatory_field_is_empty
        (String attributeValue) {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId(attributeValue);
        ServiceMandatoryFieldValidationException exception =
            assertThrows(ServiceMandatoryFieldValidationException.class, ()
                -> taskMandatoryFieldsValidator.validate(task));
        String message = exception.getMessage();
        assertTrue(message.contains("caseId cannot be null or empty"));
    }

    @Test
    @DisplayName("should throw ServiceMandatoryFieldValidationException when multiple mandatory fields are missing")
    void should_throw_service_mandatory_field_validation_exception_when_multiple_mandatory_field_are_missing() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseName(null);
        task.setCaseId("");
        ServiceMandatoryFieldValidationException exception =
            assertThrows(ServiceMandatoryFieldValidationException.class, ()
                -> taskMandatoryFieldsValidator.validate(task));

        String message = exception.getMessage();
        assertTrue(message.contains("caseId cannot be null or empty"));
        assertTrue(message.contains("caseName cannot be null or empty"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when property value cannot be found")
    void should_throw_illegal_argument_exception_when_property_value_cannot_be_found() {
        TaskResource task = getTaskResource(taskId);
        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            new LaunchDarklyFeatureFlagProvider(ldClient), true, List.of("field1", "field2"),
            jsonParserUtils);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(task));
    }

    @Test
    @DisplayName("should skip validation when mandatory field check is disabled")
    void should_skip_validation_when_mandatory_field_check_disabled() {
        TaskResource task = getTaskResource(taskId);
        TaskMandatoryFieldsValidator validator =
            new TaskMandatoryFieldsValidator(null, false,
                                             List.of("field1", "field2"), jsonParserUtils);
        assertDoesNotThrow(() -> validator.validate(task));
    }

    @Test
    @DisplayName("should skip validation when jurisdiction is excluded")
    void should_skip_validation_when_jurisdiction_excluded() {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId(null);
        task.setJurisdiction("WA");

        TaskMandatoryFieldsValidator spyValidator = Mockito.spy(taskMandatoryFieldsValidator);
        spyValidator.validate(task);

        Mockito.verify(spyValidator, Mockito.never()).validateTaskMandatoryFields(task);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when property value cannot be found")
    void should_throw_IllegalArgumentException_when_property_value_cannot_be_found() {
        TaskResource task = getTaskResource(taskId);
        TaskMandatoryFieldsValidator validator = new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider, true, List.of("invalidField"), jsonParserUtils);
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
