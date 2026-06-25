package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.ServiceMandatoryFieldValidationException;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.ROLE_CATEGORY;

/**
 * Service to validate mandatory fields of a task.
 */
@Slf4j
@Service
@SuppressWarnings({
    "PMD.CognitiveComplexity"})
public class TaskMandatoryFieldsValidator {
    private final List<String> taskMandatoryFields;

    private final List<String> tmSpecificMandatoryFields = List.of("taskId", "state","executionTypeCode","created",
                                                                   "dueDateTime", "majorPriority", "minorPriority");

    /**
     * Constructor for TaskMandatoryFieldsValidator.
     *
     * @param taskMandatoryFields             list of mandatory fields to be checked
     */
    @Autowired
    public TaskMandatoryFieldsValidator(@Value("${config.taskMandatoryFields}") List<String> taskMandatoryFields) {

        this.taskMandatoryFields = taskMandatoryFields;
    }

    /**
     * Validates the mandatory fields of a task.
     *
     * @param task the task to be validated
     */
    public void validate(TaskResource task) {
        log.info("Validating mandatory fields for task {}", task.getTaskId());
        validateTaskMandatoryFields(task);
    }

    /**
     * Validates the mandatory fields of a given task.
     *
     * @param task the task to be validated
     * @throws ServiceMandatoryFieldValidationException if any service-specific mandatory fields are null or empty
     * @throws ValidationException if any TM-specific mandatory fields are null or empty
     */
    public void validateTaskMandatoryFields(TaskResource task) {
        List<String> serviceSpecificErrors = new ArrayList<>();
        List<String> tmSpecificErrors = new ArrayList<>();

        for (String field : taskMandatoryFields) {
            try {
                Object fieldValue = PropertyUtils.getProperty(task, field);
                if (isFieldNullOrEmpty(fieldValue)) {
                    addError(field, serviceSpecificErrors, tmSpecificErrors);
                }
                if (ROLE_CATEGORY.value().equals(field) && fieldValue != null && !fieldValue.toString().isBlank()) {
                    if (!RoleCategory.isAllowed(fieldValue.toString())) {
                        addNotAllowedValuesError(field,fieldValue, serviceSpecificErrors, tmSpecificErrors);
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot find property value for mandatory field " + field, e);
            }
        }

        if (!serviceSpecificErrors.isEmpty()) {
            throw new ServiceMandatoryFieldValidationException(
                String.join(", ", serviceSpecificErrors.toString()));
        }
        if (!tmSpecificErrors.isEmpty()) {
            throw new ValidationException(String.join(", ", tmSpecificErrors.toString()));
        }
    }

    /**
     * Checks if the given field value is null or empty.
     *
     * @param fieldValue the value of the field to be checked
     * @return true if the field value is null or empty, false otherwise
     */
    private boolean isFieldNullOrEmpty(Object fieldValue) {
        if (fieldValue instanceof WorkTypeResource workTypeResource) {
            return workTypeResource.getId() == null || workTypeResource.getId().isBlank();
        } else if (fieldValue instanceof ExecutionTypeResource executionTypeResource) {
            return executionTypeResource.getExecutionCode() == null
                || executionTypeResource.getExecutionCode().getValue().isBlank();
        }
        return fieldValue == null || fieldValue.toString().isBlank();
    }

    private void addError(String field, List<String> serviceSpecificErrors, List<String> tmSpecificErrors) {
        String errorMessage = field + " cannot be null or empty";
        if (tmSpecificMandatoryFields.contains(field)) {
            tmSpecificErrors.add(errorMessage);
        } else {
            serviceSpecificErrors.add(errorMessage);
        }
    }

    private void addNotAllowedValuesError(String field, Object fieldValue, List<String> serviceSpecificErrors,
                                          List<String> tmSpecificErrors) {
        String errorMessage = field + " value '" + fieldValue + "' is not one of the allowed values";
        if (tmSpecificMandatoryFields.contains(field)) {
            tmSpecificErrors.add(errorMessage);
        } else {
            serviceSpecificErrors.add(errorMessage);
        }

    }
}
