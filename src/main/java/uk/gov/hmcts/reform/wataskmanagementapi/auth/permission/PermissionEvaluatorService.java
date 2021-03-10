package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import com.fasterxml.jackson.databind.ser.std.ObjectArraySerializer;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.VerificationResult;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.verifiers.Verifier;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PermissionEvaluatorService {

    private final List<Verifier<VerificationData>> verifiers;

    public PermissionEvaluatorService(List<Verifier<VerificationData>> verifiers) {
        this.verifiers = verifiers;
    }

    //TODO call evaluate?  Respond with a Response rather than just a boolean?
    public boolean hasAccess(Map<String, CamundaVariable> variables,
                             List<Assignment> roleAssignments,
                             List<PermissionTypes> permissionsRequired) {

        Objects.requireNonNull(roleAssignments, "Role Assignment Map cannot be null.");

        boolean hasAccess = false;
        // Loop through the roleAssignments and attempt to find a role without sufficient permissions
        for (Assignment roleAssignment : roleAssignments) {

            //Safe-guard
            if (hasAccess) {
                break;
            }

            hasAccess = evaluateAccess(
                new VerificationData(variables, roleAssignment, permissionsRequired)
            );

        }

        return hasAccess;
    }

    private boolean evaluateAccess(final VerificationData verificationData) {

        for (Verifier<VerificationData> verifier : verifiers) {
            VerificationResult result = verifier.verify(verificationData);

            //Any failure means not possible to continue
            if (!result.isVerified()) {
                return false;
            }
        }

        //// 1. Always Check Role name has required permission
        //hasAccess = hasRolePermission(
        //    permissionData.getRoleAssignment().getRoleName(),
        //    permissionData.getTaskVariables(),
        //    permissionData.getPermissionsRequired()
        //);
        //
        //if (hasAccess) {
        //    // 2. Always Check Security Classification matches the one on the task
        //    hasAccess = hasSecurityClassificationPermission(
        //        permissionData.getRoleAssignment().getClassification(),
        //        permissionData.getTaskVariables()
        //    );

        //TODO Test this and move to one of the RoleAssignment permissions checkers
        //Map<String, String> attributes = permissionData.getRoleAssignment().getAttributes();
        //Optional<Boolean> result = Arrays.stream(RequiredAttributesMatches.values())
        //    .map(requiredAttributesMatches ->
        //             new PermissionRuleChecker(camundaObjectMapper)
        //                 .checkAttributePermission(
        //                     attributes.get(requiredAttributesMatches.getRoleAssignment()),
        //                     getVariableValue(
        //                         permissionData.getTaskVariables()
        //                             .get(requiredAttributesMatches.getTaskAttribute()),
        //                         String.class
        //                     )
        //                 ))
        //    //Return only failures.
        //    .filter(aBoolean -> !aBoolean)
        //    .findAny();
        //
        ////Only false will be here.
        //if (result.isPresent()) {
        //    hasAccess = false;
        //}

        //if (roleAssignment.getAttributes() != null) {
        //
        //    Map<String, String> attributes = roleAssignment.getAttributes();
        //    // 3. Conditionally check Jurisdiction matches the one on the task
        //    String jurisdictionAttributeValue = attributes.get(RoleAttributeDefinition.JURISDICTION.value());
        //    if (hasAccess && jurisdictionAttributeValue != null) {
        //        hasAccess = hasJurisdictionPermission(jurisdictionAttributeValue, variables);
        //
        //        //TODO test code
        //        hasAccess = new PermissionRule().checkAttributePermission(
        //            jurisdictionAttributeValue,
        //            getVariableValue(variables.get(JURISDICTION.value()), String.class)
        //        );
        //    }
        //    // 4. Conditionally check CaseId matches the one on the task
        //    String caseIdAttributeValue = attributes.get(RoleAttributeDefinition.CASE_ID.value());
        //    if (hasAccess && caseIdAttributeValue != null) {
        //        hasAccess = hasCaseIdPermission(caseIdAttributeValue, variables);
        //
        //        //TODO test code
        //        hasAccess = new PermissionRule().checkAttributePermission(
        //            caseIdAttributeValue,
        //            getVariableValue(variables.get(CASE_ID.value()), String.class)
        //        );
        //
        //    }
        //    // 5. Conditionally check region matches the one on the task
        //    String regionAttributeValue = attributes.get(RoleAttributeDefinition.REGION.value());
        //    if (hasAccess && regionAttributeValue != null) {
        //        hasAccess = hasRegionPermission(regionAttributeValue, variables);
        //
        //        //TODO test code
        //        hasAccess = new PermissionRule().checkAttributePermission(
        //            regionAttributeValue,
        //            getVariableValue(variables.get(REGION.value()), String.class)
        //        );
        //
        //    }
        //    // 6. Conditionally check Location ePimms id matches the one on the task
        //    String locationAttributeValue = attributes.get(RoleAttributeDefinition.PRIMARY_LOCATION.value());
        //    if (hasAccess && locationAttributeValue != null) {
        //        hasAccess = hasLocationPermission(locationAttributeValue, variables);
        //
        //        //TODO test code
        //        hasAccess = new PermissionRule().checkAttributePermission(
        //            locationAttributeValue,
        //            getVariableValue(variables.get(LOCATION.value()), String.class)
        //        );
        //    }
        //}

        //hasAccess = hasBeginTimePermission(permissionData.getRoleAssignment(), hasAccess);
        //hasAccess = hasEndTimePermission(permissionData.getRoleAssignment(), hasAccess);

        // Passed all the verifier so give it the ok
        return true;
    }

    //private boolean hasEndTimePermission(Assignment roleAssignment, boolean hasAccess) {
    //    LocalDateTime endTime = roleAssignment.getEndTime();
    //    if (hasAccess && endTime != null) {
    //
    //        ZoneId zoneId = ZoneId.of("Europe/London");
    //        ZonedDateTime endTimeLondonTime = endTime.atZone(zoneId);
    //        ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(zoneId);
    //
    //        return currentDateTimeLondonTime.isBefore(endTimeLondonTime);
    //    }
    //    return hasAccess;
    //}
    //
    //private boolean hasBeginTimePermission(Assignment roleAssignment, boolean hasAccess) {
    //    LocalDateTime beginTime = roleAssignment.getBeginTime();
    //    if (hasAccess && beginTime != null) {
    //
    //        ZoneId zoneId = ZoneId.of("Europe/London");
    //        ZonedDateTime beginTimeLondonTime = beginTime.atZone(zoneId);
    //        ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(zoneId);
    //
    //
    //        return currentDateTimeLondonTime.isAfter(beginTimeLondonTime);
    //    }
    //    return hasAccess;
    //}
    //
    //private boolean hasLocationPermission(String roleAssignmentLocation, Map<String, CamundaVariable> variables) {
    //    String taskLocation = getVariableValue(variables.get(LOCATION.value()), String.class);
    //    return roleAssignmentLocation.equals(taskLocation);
    //}
    //
    //private boolean hasRegionPermission(String roleAssignmentRegion, Map<String, CamundaVariable> variables) {
    //    String taskRegion = getVariableValue(variables.get(REGION.value()), String.class);
    //    return roleAssignmentRegion.equals(taskRegion);
    //}
    //
    //private boolean hasCaseIdPermission(String roleAssignmentCaseId, Map<String, CamundaVariable> variables) {
    //    String taskCaseId = getVariableValue(variables.get(CASE_ID.value()), String.class);
    //    return roleAssignmentCaseId.equals(taskCaseId);
    //
    //}
    //
    //private boolean hasJurisdictionPermission(String roleAssignmentJurisdiction,
    //                                          Map<String, CamundaVariable> variables) {
    //    String taskJurisdiction = getVariableValue(variables.get(JURISDICTION.value()), String.class);
    //    return roleAssignmentJurisdiction.equals(taskJurisdiction);
    //}
    //
    ////private boolean hasJurisdictionPermission2(String roleAssignmentJurisdiction,
    ////                                           Map<String, CamundaVariable> variables) {
    ////    String taskJurisdiction = getVariableValue2(variables.get(JURISDICTION.value()), String.class)
    ////        .orElse("");
    ////    return roleAssignmentJurisdiction.equals(taskJurisdiction);
    ////}
    //
    //
    //public boolean matchRoleAttributeRule(String roleAssigmentAttribute, String taskVariable) {
    //    return roleAssigmentAttribute.equals(taskVariable);
    //
    //}
    //
    //private boolean hasSecurityClassificationPermission(Classification roleAssignmentClassification,
    //                                                    Map<String, CamundaVariable> variables) {
    //    /*
    //     * If RESTRICTED on role assignment classification, then matches all levels on task
    //     * If PRIVATE on role assignment classification then matches "PRIVATE" and "PUBLIC" on task
    //     * If PUBLIC on role assignment classification then matches "PUBLIC on task.
    //     */
    //
    //    boolean hasAccess = false;
    //    Classification taskClassification = getVariableValue(
    //        variables.get(SECURITY_CLASSIFICATION.value()),
    //        Classification.class
    //    );
    //
    //    if (taskClassification != null) {
    //        switch (roleAssignmentClassification) {
    //            case PUBLIC:
    //                hasAccess = Objects.equals(PUBLIC, taskClassification);
    //                break;
    //            case PRIVATE:
    //                hasAccess = asList(PUBLIC, PRIVATE).contains(taskClassification);
    //                break;
    //            case RESTRICTED:
    //                hasAccess = asList(PUBLIC, PRIVATE, RESTRICTED).contains(taskClassification);
    //                break;
    //            default:
    //                throw new IllegalArgumentException("Unexpected classification value");
    //        }
    //    }
    //    return hasAccess;
    //}
    //
    //
    ////Checks if RoleAssignment for user has the correct permissions for this current Operation
    ////MatchingStrategy is "at_least_one"
    ////could also be "match_all"
    //private boolean hasRolePermission(String roleName,
    //                                  Map<String, CamundaVariable> variables,
    //                                  List<PermissionTypes> permissionsRequired) {
    //    /*
    //     * Optimizations: Added safe-guards to abort early as soon as a match is found
    //     * this saves us time and further unnecessary processing
    //     */
    //    boolean hasAccess = false;
    //    //comma sep list of permissions on the RoleAssignment
    //    String taskRolePermissions = getVariableValue(variables.get(roleName), String.class);
    //    if (taskRolePermissions != null) {
    //        Set<String> taskPermissions = Arrays.stream(taskRolePermissions.split(","))
    //            .collect(Collectors.toSet());
    //        //If at least one permission is matched.
    //        for (PermissionTypes p : permissionsRequired) {
    //            //Safe-guard
    //            if (hasAccess) {
    //                break;
    //            }
    //            hasAccess = taskPermissions.contains(p.value());
    //
    //        }
    //    }
    //
    //    return hasAccess;
    //}
    //
    //private boolean hasRolePermission2(String roleName,
    //                                   Map<String, CamundaVariable> variables,
    //                                   List<PermissionTypes> permissionsRequired) {
    //    /*
    //     * Optimizations: Added safe-guards to abort early as soon as a match is found
    //     * this saves us time and further unnecessary processing
    //     */
    //    //comma sep list of permissions on the RoleAssignment
    //    final Optional<String> taskRolePermissions = getVariableValue2(variables.get(roleName), String.class);
    //    if (taskRolePermissions.isPresent()) {
    //        Set<String> taskPermissions =
    //            Arrays.stream(taskRolePermissions.get()
    //                              .split(",")
    //            )
    //                .collect(Collectors.toSet());
    //
    //        //If at least one permission is matched.
    //        for (PermissionTypes perm : permissionsRequired) {
    //            if (taskPermissions.contains(perm.value())) {
    //                return true;
    //            }
    //        }
    //    }
    //
    //    return false;
    //}

    //private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
    //    Optional<T> value = camundaObjectMapper.read(variable, type);
    //    return value.orElse(null);
    //}

}
