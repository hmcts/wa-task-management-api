package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
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
import static org.mockito.Mockito.doReturn;

@SuppressWarnings("checkstyle:LineLength")
@Slf4j
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
        doReturn(LDValue.parse("{\"jurisdictions\":[\"WA\"]}")).when(launchDarklyFeatureFlagProvider)
            .getJsonValue(any(), any());

    }

    @ParameterizedTest
    @CsvSource({
        "'', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'taskName'",
        "'someTaskName', '', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'taskType'",
        "'someTaskName', 'someTaskType', 'PUBLIC', '', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'title'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', '', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'caseId'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', '', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'caseTypeId'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', '', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'caseCategory'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', '', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'caseName'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', '', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'jurisdiction'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', '', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'region'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '', 'LEGAL_OPERATIONS', 'workTypeResource', 'location'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', '', 'workTypeResource', 'roleCategory'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', '', 'workTypeResource'",
        ", 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'taskName'",
        "'someTaskName', , 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'taskType'",
        "'someTaskName', 'someTaskType', 'PUBLIC', , 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'title'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', , 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'caseId'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', , 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'caseTypeId'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', , 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'caseCategory'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', , 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'caseName'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', , 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'jurisdiction'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', , '765324', 'LEGAL_OPERATIONS', 'workTypeResource', 'region'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', , 'LEGAL_OPERATIONS', 'workTypeResource', 'location'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', , 'workTypeResource', 'roleCategory'",
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', , 'workTypeResource'",

    })
    @DisplayName("should throw ServiceMandatoryFieldValidationException when a mandatory field is missing")
    void should_throw_service_mandatory_field_validation_exception_when_a_mandatory_field_is_missing(String taskName, String taskType, String securityClassification, String title,  String caseId, String caseTypeId, String caseCategory, String caseName, String jurisdiction, String region, String location, String roleCategory, String workTypeResource, String fieldName) {
        TaskResource task = getTaskResource(taskId, taskName, taskType, securityClassification, title, caseId, caseTypeId, caseCategory, caseName, jurisdiction, region, location, roleCategory, workTypeResource);
        ServiceMandatoryFieldValidationException exception = assertThrows(ServiceMandatoryFieldValidationException.class, () -> taskMandatoryFieldsValidator.validate(task));
        String message = exception.getMessage();
        log.info("Exception message: {}", message);
        assertTrue(message.contains(fieldName + " cannot be null or empty"));
    }

    @ParameterizedTest
    @CsvSource({
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'LEGAL_OPERATIONS', 'workTypeResource'",
        })
    @DisplayName("should validate successfully when all mandatory fields are present")
    void should_validate_successfully_when_all_mandatory_fields_are_present(String taskName, String taskType, String securityClassification, String title,  String caseId, String caseTypeId, String caseCategory, String caseName, String jurisdiction, String region, String location, String roleCategory, String workTypeResource) {
        TaskResource task = getTaskResource(taskId, taskName, taskType, securityClassification, title, caseId, caseTypeId, caseCategory, caseName, jurisdiction, region, location, roleCategory, workTypeResource);
        assertDoesNotThrow(() -> taskMandatoryFieldsValidator.validate(task));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("should throw ServiceMandatoryFieldValidationException when a mandatory field is missing and field name present in message")
    void should_throw_service_mandatory_field_validation_exception_when_a_mandatory_field_is_missing_and_field_name_present_in_message(String attributeValue) {
        TaskResource task = getTaskResource(taskId);
        task.setCaseId(attributeValue);
        ServiceMandatoryFieldValidationException exception =
            assertThrows(ServiceMandatoryFieldValidationException.class, ()
                -> taskMandatoryFieldsValidator.validate(task));
        String message = exception.getMessage();
        assertTrue(message.contains("caseId cannot be null or empty"));
    }

    @ParameterizedTest
    @CsvSource({
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', '', 'workTypeResource'",
    })
    @DisplayName("should throw ServiceMandatoryFieldValidationException when role category is missing")
    void should_throw_service_mandatory_field_validation_exception_when_role_category_is_missing(String taskName, String taskType, String securityClassification, String title,  String caseId, String caseTypeId, String caseCategory, String caseName, String jurisdiction, String region, String location,String roleCategory, String workTypeResource) {
        TaskResource task = getTaskResource(taskId, taskName, taskType, securityClassification, title, caseId, caseTypeId, caseCategory, caseName, jurisdiction, region, location, roleCategory, workTypeResource);
        ServiceMandatoryFieldValidationException exception =
            assertThrows(ServiceMandatoryFieldValidationException.class, ()
                -> taskMandatoryFieldsValidator.validate(task));

        String message = exception.getMessage();
        assertTrue(message.contains("roleCategory cannot be null or empty"));
    }

    @ParameterizedTest
    @CsvSource({
        "'someTaskName', 'someTaskType', 'PUBLIC', 'title', 'CASE_ID', 'Asylum', 'CaseCategory', 'CaseName', 'IA', 'TestRegion', '765324', 'wrongRoleCategory', 'workTypeResource'",
    })
    @DisplayName("should throw ServiceMandatoryFieldValidationException when role category is incorrect")
    void should_throw_service_mandatory_field_validation_exception_when_role_category_is_incorrect(String taskName, String taskType, String securityClassification, String title,  String caseId, String caseTypeId, String caseCategory, String caseName, String jurisdiction, String region, String location,String roleCategory, String workTypeResource) {
        TaskResource task = getTaskResource(taskId, taskName, taskType, securityClassification, title, caseId, caseTypeId, caseCategory, caseName, jurisdiction, region, location, roleCategory, workTypeResource);
        ServiceMandatoryFieldValidationException exception =
            assertThrows(ServiceMandatoryFieldValidationException.class, ()
                -> taskMandatoryFieldsValidator.validate(task));

        String message = exception.getMessage();
        assertTrue(message.contains("roleCategory value 'wrongRoleCategory' is not one of the allowed values"));
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
            launchDarklyFeatureFlagProvider, true, List.of("field1", "field2"),
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
        return getTaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            "PUBLIC",
            "title",
            "CASE_ID",
            "Asylum",
            "CaseCategory",
            "CaseName",
            "IA",
            "TestRegion",
            "765324"
        );
    }

    private static TaskResource getTaskResource(String taskId, String taskName, String taskType, String securityClassification, String title,  String caseId, String caseTypeId, String caseCategory, String caseName, String jurisdiction, String region, String location) {
        final TaskResource taskResource = new TaskResource(
            taskId,
            taskName,
            taskType,
            CFTTaskState.UNASSIGNED
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseTypeId);
        taskResource.setSecurityClassification(SecurityClassification.valueOf(securityClassification));
        taskResource.setLocation(location);
        taskResource.setRegion(region);
        taskResource.setCaseId(caseId);
        taskResource.setTitle(title);
        taskResource.setMajorPriority(2000);
        taskResource.setMinorPriority(500);
        taskResource.setExecutionTypeCode(
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"));
        taskResource.setCaseCategory(caseCategory);
        taskResource.setCaseName(caseName);
        return taskResource;
    }

    private static TaskResource getTaskResource(String taskId, String taskName, String taskType, String securityClassification, String title,  String caseId, String caseTypeId, String caseCategory, String caseName, String jurisdiction, String region, String location, String roleCategory, String workTypeResource) {
        TaskResource taskResource = getTaskResource(
            taskId,
            taskName,
            taskType,
            securityClassification,
            title,
            caseId,
            caseTypeId,
            caseCategory,
            caseName,
            jurisdiction,
            region,
            location
        );
        taskResource.setRoleCategory(roleCategory);
        taskResource.setWorkTypeResource(new WorkTypeResource((String) workTypeResource, StringUtils.EMPTY));
        return taskResource;
    }
}
