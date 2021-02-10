package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;

@Slf4j
@Service
@SuppressWarnings({
    "java:S3776",
    "PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter", "PMD.AvoidDeeplyNestedIfStmts", "PMD.CyclomaticComplexity"
})
public class PermissionEvaluatorService {

    private final CamundaObjectMapper camundaObjectMapper;

    @Autowired
    public PermissionEvaluatorService(CamundaObjectMapper camundaObjectMapper) {
        this.camundaObjectMapper = camundaObjectMapper;
    }

    public boolean hasAccess(Map<String, CamundaVariable> variables,
                             List<Assignment> roleAssignments,
                             List<PermissionTypes> permissionsRequired) {

        boolean hasAccess = false;
        // Loop through the roleAssignments and attempt to find a role with sufficient permissions
        for (Assignment roleAssignment : roleAssignments) {
            //Safe-guard
            if (hasAccess) {
                break;
            }
            hasAccess = evaluateAccess(variables, roleAssignment, permissionsRequired);
        }
        return hasAccess;
    }

    private boolean evaluateAccess(Map<String, CamundaVariable> variables,
                                   Assignment roleAssignment,
                                   List<PermissionTypes> permissionsRequired) {
        boolean hasAccess;

        // 1. Always Check Role name has required permission
        hasAccess = hasRolePermission(roleAssignment.getRoleName(), variables, permissionsRequired);

        if (hasAccess) {
            // 2. Always Check Security Classification matches the one on the task
            hasAccess = hasSecurityClassificationPermission(
                roleAssignment.getClassification(),
                variables
            );

            if (roleAssignment.getAttributes() != null) {

                Map<String, String> attributes = roleAssignment.getAttributes();
                // 3. Conditionally check Jurisdiction matches the one on the task
                String jurisdictionAttributeValue = attributes.get(RoleAttributeDefinition.JURISDICTION.value());
                if (hasAccess && jurisdictionAttributeValue != null) {
                    hasAccess = hasJurisdictionPermission(jurisdictionAttributeValue, variables);
                }
                // 4. Conditionally check CaseId matches the one on the task
                String caseIdAttributeValue = attributes.get(RoleAttributeDefinition.CASE_ID.value());
                if (hasAccess && caseIdAttributeValue != null) {
                    hasAccess = hasCaseIdPermission(caseIdAttributeValue, variables);
                }
                // 5. Conditionally check region matches the one on the task
                String regionAttributeValue = attributes.get(RoleAttributeDefinition.REGION.value());
                if (hasAccess && regionAttributeValue != null) {
                    hasAccess = hasRegionPermission(regionAttributeValue, variables);
                }
                // 6. Conditionally check Location ePimms id matches the one on the task
                String locationAttributeValue = attributes.get(RoleAttributeDefinition.PRIMARY_LOCATION.value());
                if (hasAccess && locationAttributeValue != null) {
                    hasAccess = hasLocationPermission(locationAttributeValue, variables);
                }
            }

            hasAccess = hasBeginTimePermission(roleAssignment, hasAccess);
            hasAccess = hasEndTimePermission(roleAssignment, hasAccess);
        }

        return hasAccess;
    }

    private boolean hasEndTimePermission(Assignment roleAssignment, boolean hasAccess) {
        LocalDateTime endTime = roleAssignment.getEndTime();
        if (hasAccess && endTime != null) {

            ZoneId zoneId = ZoneId.of("Europe/London");
            ZonedDateTime endTimeLondonTime = endTime.atZone(zoneId);
            ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(zoneId);

            return currentDateTimeLondonTime.isBefore(endTimeLondonTime);
        }
        return hasAccess;
    }

    private boolean hasBeginTimePermission(Assignment roleAssignment, boolean hasAccess) {
        LocalDateTime beginTime = roleAssignment.getBeginTime();
        if (hasAccess && beginTime != null) {

            ZoneId zoneId = ZoneId.of("Europe/London");
            ZonedDateTime beginTimeLondonTime = beginTime.atZone(zoneId);
            ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(zoneId);

            return currentDateTimeLondonTime.isAfter(beginTimeLondonTime);
        }
        return hasAccess;
    }

    private boolean hasLocationPermission(String roleAssignmentLocation, Map<String, CamundaVariable> variables) {
        String taskLocation = getVariableValue(variables.get(LOCATION.value()), String.class);
        return roleAssignmentLocation.equals(taskLocation);
    }

    private boolean hasRegionPermission(String roleAssignmentRegion, Map<String, CamundaVariable> variables) {
        String taskRegion = getVariableValue(variables.get(REGION.value()), String.class);
        return roleAssignmentRegion.equals(taskRegion);
    }

    private boolean hasCaseIdPermission(String roleAssignmentCaseId, Map<String, CamundaVariable> variables) {
        String taskCaseId = getVariableValue(variables.get(CASE_ID.value()), String.class);
        return roleAssignmentCaseId.equals(taskCaseId);

    }

    private boolean hasJurisdictionPermission(String roleAssignmentJurisdiction,
                                              Map<String, CamundaVariable> variables) {
        String taskJurisdiction = getVariableValue(variables.get(JURISDICTION.value()), String.class);
        return roleAssignmentJurisdiction.equals(taskJurisdiction);
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

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }

}
