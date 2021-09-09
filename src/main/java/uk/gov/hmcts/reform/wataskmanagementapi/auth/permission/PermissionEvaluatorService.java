package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PRIVATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PUBLIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.RESTRICTED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;

@Slf4j
@Service
@SuppressWarnings({
    "PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter","PMD.GodClass","PMD.CyclomaticComplexity","PMD.TooManyMethods"
})
public class PermissionEvaluatorService {

    private final CamundaObjectMapper camundaObjectMapper;
    private final AttributesValueVerifier attributeEvaluatorService;

    @Autowired
    public PermissionEvaluatorService(CamundaObjectMapper camundaObjectMapper,
                                      AttributesValueVerifier attributeEvaluatorService) {
        this.camundaObjectMapper = camundaObjectMapper;
        this.attributeEvaluatorService = attributeEvaluatorService;
    }


    /**
     * This method evaluates access to the task but also checks that a task was assigned to a user.
     * Note: Uses a hierarchy is the user is a tribunal case worker he might be able to perform actions.
     *
     * @param taskAssignee        the task assignee if any
     * @param userId              the user id performing the action
     * @param variables           the task variables
     * @param roleAssignments     the role assignments of the user performing the action
     * @param permissionsRequired permissions required to perform this action
     * @return whether the user should be able to perform this action
     */
    public boolean hasAccessWithAssigneeCheckAndHierarchy(String taskAssignee,
                                                          String userId,
                                                          Map<String, CamundaVariable> variables,
                                                          List<RoleAssignment> roleAssignments,
                                                          List<PermissionTypes> permissionsRequired) {

        boolean hasAccess = false;
        // Loop through the roleAssignments and attempt to find a role with sufficient permissions
        for (RoleAssignment roleAssignment : roleAssignments) {
            //Safe-guard
            if (hasAccess) {
                break;
            }

            // If a user is a senior-tribunal-caseworker he might still be able to perform actions on it
            if ("senior-tribunal-caseworker".equals(roleAssignment.getRoleName())) {
                hasAccess = evaluateAccess(variables, roleAssignment, permissionsRequired);
            } else {
                hasAccess = checkAccessWithAssignee(
                    taskAssignee,
                    userId,
                    variables,
                    roleAssignment,
                    permissionsRequired
                );
            }
        }
        return hasAccess;
    }

    public boolean hasAccess(Map<String, CamundaVariable> variables,
                             List<RoleAssignment> roleAssignments,
                             List<PermissionTypes> permissionsRequired) {

        boolean hasAccess = false;
        // Loop through the roleAssignments and attempt to find a role with sufficient permissions
        for (RoleAssignment roleAssignment : roleAssignments) {
            //Safe-guard
            if (hasAccess) {
                break;
            }
            hasAccess = evaluateAccess(variables, roleAssignment, permissionsRequired);
        }
        return hasAccess;
    }

    private boolean checkAccessWithAssignee(String taskAssignee,
                                            String userId,
                                            Map<String, CamundaVariable> variables,
                                            RoleAssignment roleAssignment,
                                            List<PermissionTypes> permissionsRequired) {
        boolean hasAccess = false;
        if (taskAssignee != null && taskAssignee.equals(userId)) {
            hasAccess = evaluateAccess(variables, roleAssignment, permissionsRequired);
        }
        return hasAccess;
    }

    private boolean evaluateAccess(Map<String, CamundaVariable> variables,
                                   RoleAssignment roleAssignment,
                                   List<PermissionTypes> permissionsRequired) {
        boolean hasAccess;

        log.debug("Evaluating access for {}", roleAssignment);
        // 1. Always Check Role name has required permission
        hasAccess = hasRolePermission(roleAssignment.getRoleName(), variables, permissionsRequired);
        log.debug("RoleName permission check {}", hasAccess);

        if (hasAccess) {
            // 2. Always Check Security Classification matches the one on the task
            hasAccess = hasSecurityClassificationPermission(
                roleAssignment.getClassification(),
                variables
            );
            log.debug("Security Classification permission check {}", hasAccess);
            if (roleAssignment.getAttributes() != null && hasAccess) {
                hasAccess = attributesPermissionCheck(variables, roleAssignment);
            }

            hasAccess = hasBeginTimePermission(roleAssignment, hasAccess);
            log.debug("BeginTime permission check {}", hasAccess);
            hasAccess = hasEndTimePermission(roleAssignment, hasAccess);
            log.debug("EndTime permission check {}", hasAccess);
        }

        return hasAccess;
    }

    private boolean evaluateRoleAccess(TaskResource taskResource,
                                       RoleAssignment roleAssignment,
                                       List<PermissionTypes> permissionsRequired) {
        boolean hasAccess;
        log.debug("Evaluating access for {}", roleAssignment);
        // 1. Always Check Role name has required permission
        hasAccess = checkRolePermission(roleAssignment.getRoleName(), taskResource, permissionsRequired);
        log.debug("RoleName permission check {}", hasAccess);
        // 1. Always Check Role name has required permission
        hasAccess = checkRoleAuthorisations(roleAssignment, taskResource);
        log.debug("RoleName permission check {}", hasAccess);

        if (hasAccess) {
            // 2. Always Check Security Classification matches the one on the task
            hasAccess = checkSecurityClassificationPermission(
                roleAssignment.getClassification(),
                taskResource
            );
            log.debug("Security Classification permission check {}", hasAccess);
            if (roleAssignment.getAttributes() != null && hasAccess) {
                hasAccess = checkAttributesPermission(taskResource, roleAssignment);
            }

            hasAccess = hasBeginTimePermission(roleAssignment, hasAccess);
            log.debug("BeginTime permission check {}", hasAccess);
            hasAccess = hasEndTimePermission(roleAssignment, hasAccess);
            log.debug("EndTime permission check {}", hasAccess);
        }

        return hasAccess;
    }

    private boolean checkAttributesPermission(TaskResource taskResource, RoleAssignment roleAssignment) {
        boolean hasAccess = true;
        Map<String, String> attributes = roleAssignment.getAttributes();
        // 3. Conditionally check Jurisdiction matches the one on the task
        String jurisdictionAttributeValue = attributes.get(RoleAttributeDefinition.JURISDICTION.value());
        if (jurisdictionAttributeValue != null) {
            hasAccess = jurisdictionAttributeValue.equals(taskResource.getJurisdiction());
            log.debug("Jurisdiction permission check {}", hasAccess);
        }
        // 4. Conditionally check region matches the one on the task
        String regionAttributeValue = attributes.get(RoleAttributeDefinition.REGION.value());
        if (hasAccess && regionAttributeValue != null) {
            hasAccess = regionAttributeValue.equals(taskResource.getRegion());
            log.debug("Region permission check {}", hasAccess);
        }
        // 5. Conditionally check baseLocation id matches the one on the task
        String locationAttributeValue = attributes.get(RoleAttributeDefinition.BASE_LOCATION.value());
        if (hasAccess && locationAttributeValue != null) {
            hasAccess = locationAttributeValue.equals(taskResource.getLocation());
            log.debug("Location permission check {}", hasAccess);
        }

        if (hasAccess) {
            // 6. Conditionally check CaseId matches the one on the task
            String caseIdAttributeValue = attributes.get(RoleAttributeDefinition.CASE_ID.value());
            if (caseIdAttributeValue != null) {
                hasAccess = taskResource.getCaseId().equals(caseIdAttributeValue);
                log.debug("CaseId permission check {}", hasAccess);
            }
            // 7. Conditionally check caseTypeId matches the one on the task
            String caseTypeValue = attributes.get(RoleAttributeDefinition.CASE_TYPE.value());
            if (hasAccess && caseTypeValue != null) {
                hasAccess = taskResource.getCaseTypeId().equals(caseTypeValue);
                log.debug("CaseTypeId permission check {}", hasAccess);
            }

        }
        return hasAccess;
    }

    private boolean checkSecurityClassificationPermission(Classification roleAssignmentClassification,
                                                          TaskResource taskResource) {
        /*
         * If RESTRICTED on role assignment classification, then matches all levels on task
         * If PRIVATE on role assignment classification then matches "PRIVATE" and "PUBLIC" on task
         * If PUBLIC on role assignment classification then matches "PUBLIC on task.
         */

        boolean hasAccess = false;

        if (taskResource.getSecurityClassification() != null) {
            switch (roleAssignmentClassification) {
                case PUBLIC:
                    hasAccess = Objects.equals(PUBLIC, taskResource.getSecurityClassification());
                    break;
                case PRIVATE:
                    hasAccess = asList(PUBLIC, PRIVATE).contains(taskResource.getSecurityClassification());
                    break;
                case RESTRICTED:
                    hasAccess = asList(PUBLIC, PRIVATE, RESTRICTED).contains(taskResource.getSecurityClassification());
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected classification value");
            }
        }
        return hasAccess;
    }

    private boolean attributesPermissionCheck(Map<String, CamundaVariable> variables,
                                              RoleAssignment roleAssignment) {
        boolean hasAccess = true;
        Map<String, String> attributes = roleAssignment.getAttributes();
        // 3. Conditionally check Jurisdiction matches the one on the task
        String jurisdictionAttributeValue = attributes.get(RoleAttributeDefinition.JURISDICTION.value());
        if (jurisdictionAttributeValue != null) {
            hasAccess = attributeEvaluatorService.hasJurisdictionPermission(
                jurisdictionAttributeValue, variables);
            log.debug("Jurisdiction permission check {}", hasAccess);
        }
        // 4. Conditionally check region matches the one on the task
        String regionAttributeValue = attributes.get(RoleAttributeDefinition.REGION.value());
        if (hasAccess && regionAttributeValue != null) {
            hasAccess = attributeEvaluatorService.hasRegionPermission(regionAttributeValue, variables);
            log.debug("Region permission check {}", hasAccess);
        }
        // 5. Conditionally check baseLocation id matches the one on the task
        String locationAttributeValue = attributes.get(RoleAttributeDefinition.BASE_LOCATION.value());
        if (hasAccess && locationAttributeValue != null) {
            hasAccess = attributeEvaluatorService.hasLocationPermission(locationAttributeValue, variables);
            log.debug("Location permission check {}", hasAccess);
        }

        if (hasAccess) {
            hasAccess = restrictedAttributesCheck(variables, attributeEvaluatorService, attributes);
        }
        return hasAccess;
    }

    private boolean restrictedAttributesCheck(Map<String, CamundaVariable> variables,
                                              AttributesValueVerifier attributeEvaluatorService,
                                              Map<String, String> attributes) {
        boolean hasAccess = true;
        // 6. Conditionally check CaseId matches the one on the task
        String caseIdAttributeValue = attributes.get(RoleAttributeDefinition.CASE_ID.value());
        if (caseIdAttributeValue != null) {
            hasAccess = attributeEvaluatorService.hasCaseIdPermission(caseIdAttributeValue, variables);
            log.debug("CaseId permission check {}", hasAccess);
        }
        // 7. Conditionally check caseTypeId matches the one on the task
        String caseTypeValue = attributes.get(RoleAttributeDefinition.CASE_TYPE.value());
        if (hasAccess && caseTypeValue != null) {
            hasAccess = attributeEvaluatorService.hasCaseTypeIdPermission(caseTypeValue, variables);
            log.debug("CaseTypeId permission check {}", hasAccess);
        }
        return hasAccess;
    }

    private boolean hasEndTimePermission(RoleAssignment roleAssignment, boolean hasAccess) {
        LocalDateTime endTime = roleAssignment.getEndTime();
        if (hasAccess && endTime != null) {

            ZoneId zoneId = ZoneId.of("Europe/London");
            ZonedDateTime endTimeLondonTime = endTime.atZone(zoneId);
            ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(zoneId);

            return currentDateTimeLondonTime.isBefore(endTimeLondonTime);
        }
        return hasAccess;
    }

    private boolean hasBeginTimePermission(RoleAssignment roleAssignment, boolean hasAccess) {
        LocalDateTime beginTime = roleAssignment.getBeginTime();
        if (hasAccess && beginTime != null) {

            ZoneId zoneId = ZoneId.of("Europe/London");
            ZonedDateTime beginTimeLondonTime = beginTime.atZone(zoneId);
            ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(zoneId);

            return currentDateTimeLondonTime.isAfter(beginTimeLondonTime);
        }
        return hasAccess;
    }

    private boolean hasSecurityClassificationPermission(Classification roleAssignmentClassification,
                                                        Map<String, CamundaVariable> variables) {
        /*
         * If RESTRICTED on role assignment classification, then matches all levels on task
         * If PRIVATE on role assignment classification then matches "PRIVATE" and "PUBLIC" on task
         * If PUBLIC on role assignment classification then matches "PUBLIC on task.
         */

        boolean hasAccess = false;
        Classification taskClassification = getVariableValue(
            variables.get(SECURITY_CLASSIFICATION.value()),
            Classification.class
        );

        if (taskClassification != null) {
            switch (roleAssignmentClassification) {
                case PUBLIC:
                    hasAccess = Objects.equals(PUBLIC, taskClassification);
                    break;
                case PRIVATE:
                    hasAccess = asList(PUBLIC, PRIVATE).contains(taskClassification);
                    break;
                case RESTRICTED:
                    hasAccess = asList(PUBLIC, PRIVATE, RESTRICTED).contains(taskClassification);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected classification value");
            }
        }
        return hasAccess;
    }

    private boolean hasRolePermission(String roleName,
                                      Map<String, CamundaVariable> variables,
                                      List<PermissionTypes> permissionsRequired) {
        /*
         * Optimizations: Added safe-guards to abort early as soon as a match is found
         * this saves us time and further unnecessary processing
         */
        boolean hasAccess = false;
        String taskRolePermissions = getVariableValue(variables.get(roleName), String.class);
        if (taskRolePermissions != null) {
            Set<String> taskPermissions = Arrays.stream(taskRolePermissions.split(","))
                .collect(Collectors.toSet());
            for (PermissionTypes p : permissionsRequired) {
                //Safe-guard
                if (hasAccess) {
                    break;
                }
                hasAccess = taskPermissions.contains(p.value());

            }
        }

        return hasAccess;
    }

    private boolean checkRolePermission(String roleName,
                                      TaskResource taskResource,
                                      List<PermissionTypes> permissionsRequired) {
        /*
         * Optimizations: Added safe-guards to abort early as soon as a match is found
         * this saves us time and further unnecessary processing
         */
        boolean hasAccess = false;

        Set<TaskRoleResource> taskRolePermissions = taskResource.getTaskRoleResources()
            .stream().filter(taskRoleResource -> taskRoleResource.getRoleName().equals(roleName))
            .collect(Collectors.toSet());

        if (taskRolePermissions != null) {
            for (PermissionTypes p : permissionsRequired) {
                //Safe-guard
                if (hasAccess) {
                    break;
                }

                for (TaskRoleResource taskRoleResource : taskRolePermissions) {
                    switch (p) {
                        case READ:
                            hasAccess = taskRoleResource.getRead();
                            break;
                        case REFER:
                            hasAccess = taskRoleResource.getRefer();
                            break;
                        case OWN:
                            hasAccess = taskRoleResource.getOwn();
                            break;
                        case MANAGE:
                            hasAccess = taskRoleResource.getManage();
                            break;
                        case EXECUTE:
                            hasAccess = taskRoleResource.getExecute();
                            break;
                        case CANCEL:
                            hasAccess = taskRoleResource.getCancel();
                            break;
                        default:
                            throw new IllegalArgumentException("Unexpected permission value");
                    }
                }
            }
        }
        return hasAccess;
    }

    private boolean checkRoleAuthorisations(RoleAssignment roleAssignment,
                                      TaskResource taskResource) {
        /*
         * Optimizations: Added safe-guards to abort early as soon as a match is found
         * this saves us time and further unnecessary processing
         */
        boolean hasAccess = roleAssignment.getAuthorisations().isEmpty() ? true : false;
        if (hasAccess) {
            return hasAccess;
        }

        Set<TaskRoleResource> taskRolePermissions = taskResource.getTaskRoleResources()
            .stream().filter(taskRoleResource -> taskRoleResource.getRoleName().equals(roleAssignment.getRoleName()))
            .collect(Collectors.toSet());

        if (taskRolePermissions != null) {
            for (String p : roleAssignment.getAuthorisations()) {
                //Safe-guard
                if (hasAccess) {
                    break;
                }

                for (TaskRoleResource taskRoleResource : taskRolePermissions) {
                    hasAccess = asList(taskRoleResource.getAuthorizations()).contains(p);
                }
            }
        }
        return hasAccess;
    }

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }

    public boolean checkAccess(TaskResource taskResource,
                               List<RoleAssignment> roleAssignments,
                               List<PermissionTypes> permissionsRequired) {
        boolean hasAccess = false;
        // Loop through the roleAssignments and attempt to find a role with sufficient permissions
        for (RoleAssignment roleAssignment : roleAssignments) {
            //Safe-guard
            if (hasAccess) {
                break;
            }
            hasAccess = evaluateRoleAccess(taskResource, roleAssignment, permissionsRequired);
        }
        return hasAccess;
    }
}
