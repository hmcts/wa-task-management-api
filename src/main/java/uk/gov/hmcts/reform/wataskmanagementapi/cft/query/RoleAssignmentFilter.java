package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class RoleAssignmentFilter {

    public static Predicate checkRoleAssignmentPermissions(
        Root<TaskResource> root,
        Join<TaskResource, TaskRoleResource> taskRoleResources,
        CriteriaBuilder builder,
        RoleAssignment roleAssignment) {
        Predicate roleName = builder.equal(taskRoleResources.get("roleName"), roleAssignment.getRoleName());
        CriteriaBuilder.In<Object> securityClassification = mapSecurityClassification(
            root, builder, roleAssignment
        );
        final Predicate basicPredicate = searchByBasicGrantType(root, taskRoleResources, builder, roleAssignment);
        final Predicate specificPredicate = searchBySpecificGrantType(root, taskRoleResources, builder, roleAssignment);
        final Predicate standardPredicate = searchByStandardGrantType(root, taskRoleResources, builder, roleAssignment);
        final Predicate challengedPredicate = searchByChallengedGrantType(root, taskRoleResources, builder, roleAssignment);

        final Predicate grantTypePredicates = builder.or(specificPredicate,
            standardPredicate, challengedPredicate);

        return builder.and(roleName, securityClassification,grantTypePredicates);
    }

    private static CriteriaBuilder.In<Object> mapSecurityClassification(
        Root<TaskResource> root,
        CriteriaBuilder builder,
        RoleAssignment roleAssignment) {
        final Classification classification = roleAssignment.getClassification();
        if (classification.equals(Classification.RESTRICTED)) {
            return builder.in(root.get("securityClassification")).value(
                List.of(
                    SecurityClassification.RESTRICTED,
                    SecurityClassification.PRIVATE,
                    SecurityClassification.PUBLIC
                )
            );

        } else if (classification.equals(Classification.PRIVATE)) {
            return builder.in(root.get("securityClassification")).value(
                List.of(
                    SecurityClassification.PRIVATE,
                    SecurityClassification.PUBLIC
                )
            );
        } else {
            return builder.in(root.get("securityClassification")).value(
                List.of(
                    SecurityClassification.PUBLIC
                )
            );
        }
    }

    private static Predicate mapAuthorizations(Join<TaskResource, TaskRoleResource> taskRoleResources,
                                              CriteriaBuilder builder,
                                              RoleAssignment roleAssignment) {
        Predicate nullAuthorizations = taskRoleResources.get("authorizations").in((Object) new String[]{});
        if (roleAssignment.getAuthorisations() != null) {
            Predicate authorizations = taskRoleResources.get("authorizations").in(
                (Object) roleAssignment.getAuthorisations().toArray()
            );
            return builder.or(nullAuthorizations, authorizations);

        }
        return nullAuthorizations;
    }

    private static Predicate searchByCaseTypeId(Root<TaskResource> root,
                                               CriteriaBuilder builder,
                                               RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String caseTypeValue = roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_TYPE.value());
            if (StringUtils.hasText(caseTypeValue)) {
                return builder.equal(root.get("caseTypeId"), caseTypeValue);
            }
        }
        return builder.conjunction();
    }

    private static Predicate searchByRegion(Root<TaskResource> root,
                                           CriteriaBuilder builder,
                                           RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String regionVal = roleAssignment.getAttributes().get(RoleAttributeDefinition.REGION.value());
            if (StringUtils.hasText(regionVal)) {
                return builder.equal(root.get("region"), regionVal);
            }
        }
        return builder.conjunction();
    }

    private static Predicate searchByExcludingCaseId(Root<TaskResource> root,
                                                    CriteriaBuilder builder,
                                                    RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String caseId = roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_ID.value());
            if (StringUtils.hasText(caseId)) {
                return builder.notEqual(root.get("caseId"), caseId);
            }
        }
        return builder.conjunction();
    }

    private static Predicate searchByBasicGrantType(Root<TaskResource> root,
                                                    final Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                    CriteriaBuilder builder,
                                                    RoleAssignment roleAssignment) {
        Predicate emptyAuthorizations = taskRoleResources.get("authorizations").in((Object) new String[]{});

        return builder.and(emptyAuthorizations);
    }

    private static Predicate searchBySpecificGrantType(Root<TaskResource> root,
                                                       final Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                       CriteriaBuilder builder,
                                                       RoleAssignment roleAssignment) {
        Predicate emptyAuthorizations = taskRoleResources.get("authorizations").in((Object) new String[]{});
        Predicate caseTypeId = searchByCaseTypeId(root, builder, roleAssignment);
        Predicate jurisdiction = searchByRoleJurisdiction(root, builder, roleAssignment);
        Predicate includedCaseId = searchByIncludingCaseId(root, builder, roleAssignment);
        return builder.and(emptyAuthorizations, caseTypeId, jurisdiction, includedCaseId);
    }

    private static Predicate searchByStandardGrantType(Root<TaskResource> root,
                                                       final Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                     CriteriaBuilder builder,
                                                     RoleAssignment roleAssignment) {
        Predicate emptyAuthorizations = taskRoleResources.get("authorizations").in((Object) new String[]{});
        Predicate region = searchByRegion(root, builder, roleAssignment);
        Predicate jurisdiction = searchByRoleJurisdiction(root, builder, roleAssignment);
        Predicate location = searchByRoleLocation(root, builder, roleAssignment);
        Predicate excludedCaseId = searchByExcludingCaseId(root, builder, roleAssignment);

        return builder.and(emptyAuthorizations, region, jurisdiction, location, excludedCaseId);
    }

    private static Predicate searchByChallengedGrantType(Root<TaskResource> root,
                                                         final Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                       CriteriaBuilder builder,
                                                       RoleAssignment roleAssignment) {
        final Predicate authorizations = mapAuthorizations(taskRoleResources, builder, roleAssignment);
        Predicate jurisdiction = searchByRoleJurisdiction(root, builder, roleAssignment);
        Predicate excludedCaseId = searchByExcludingCaseId(root, builder, roleAssignment);

        return builder.and(authorizations, jurisdiction, excludedCaseId);
    }

    private static Predicate searchByIncludingCaseId(Root<TaskResource> root,
                                                     CriteriaBuilder builder,
                                                     RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String caseId = roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_ID.value());
            if (StringUtils.hasText(caseId)) {
                return builder.equal(root.get("caseId"), caseId);
            }
        }
        return builder.conjunction();
    }

    private static Predicate searchByRoleJurisdiction(Root<TaskResource> root,
                                                     CriteriaBuilder builder,
                                                     RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String regionVal = roleAssignment.getAttributes().get(RoleAttributeDefinition.JURISDICTION.value());
            if (StringUtils.hasText(regionVal)) {
                return builder.equal(root.get("jurisdiction"), regionVal);
            }
        }
        return builder.conjunction();
    }

    private static Predicate searchByRoleLocation(Root<TaskResource> root,
                                                 CriteriaBuilder builder,
                                                 RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String regionVal = roleAssignment.getAttributes().get(RoleAttributeDefinition.BASE_LOCATION.value());
            if (StringUtils.hasText(regionVal)) {
                return builder.equal(root.get("location"), regionVal);
            }
        }
        return builder.conjunction();
    }
}
