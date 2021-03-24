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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;

@Slf4j
@Service
@SuppressWarnings({
    "PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter"
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
        AttributesValueVerifier attributeEvaluatorService =
            new AttributesValueVerifier(camundaObjectMapper);

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
                hasAccess = attributesPermissionCheck(variables, roleAssignment, attributeEvaluatorService);
            }

            hasAccess = hasBeginTimePermission(roleAssignment, hasAccess);
            log.debug("BeginTime permission check {}", hasAccess);
            hasAccess = hasEndTimePermission(roleAssignment, hasAccess);
            log.debug("EndTime permission check {}", hasAccess);
        }

        return hasAccess;
    }

    private boolean attributesPermissionCheck(Map<String, CamundaVariable> variables,
                                              Assignment roleAssignment,
                                              AttributesValueVerifier attributeEvaluatorService) {
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
