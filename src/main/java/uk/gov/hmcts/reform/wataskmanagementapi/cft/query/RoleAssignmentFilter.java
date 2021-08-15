package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@SuppressWarnings("PMD.LawOfDemeter")
public final class RoleAssignmentFilter {

    public static final String CASE_ID = "caseId";
    public static final String JURISDICTION = "jurisdiction";
    public static final String LOCATION = "location";
    public static final String REGION = "region";
    public static final String CASE_TYPE_ID = "caseTypeId";
    public static final String AUTHORIZATIONS = "authorizations";
    public static final String SECURITY_CLASSIFICATION = "securityClassification";
    public static final String ROLE_NAME = "roleName";

    private RoleAssignmentFilter() {
        // avoid creating object
    }

    public static Predicate checkRoleAssignmentPermissions(
        Root<TaskResource> root,
        Join<TaskResource, TaskRoleResource> taskRoleResources,
        CriteriaBuilder builder,
        RoleAssignment roleAssignment) {
        Predicate roleName = builder.equal(taskRoleResources.get(ROLE_NAME), roleAssignment.getRoleName());
        CriteriaBuilder.In<Object> securityClassification = mapSecurityClassification(
            root, builder, roleAssignment
        );
        Predicate authorizationsPredicate = mapAuthorizations(taskRoleResources, builder, roleAssignment);
        Predicate caseTypeId = searchByCaseTypeId(root, builder, roleAssignment);
        Predicate region = searchByRegion(root, builder, roleAssignment);
        Predicate jurisdiction = searchByRoleJurisdiction(root, builder, roleAssignment);
        Predicate location = searchByRoleLocation(root, builder, roleAssignment);
        Predicate caseId;
        if (GrantType.CHALLENGED.equals(roleAssignment.getGrantType())
            || GrantType.STANDARD.equals(roleAssignment.getGrantType())) {
            caseId = searchByExcludingCaseId(root, builder, roleAssignment);
        } else {
            caseId = searchByIncludingCaseId(root, builder, roleAssignment);
        }

        return builder.and(
            roleName, securityClassification, authorizationsPredicate,
            caseTypeId, region, caseId, jurisdiction, location
        );
    }

    private static CriteriaBuilder.In<Object> mapSecurityClassification(
        Root<TaskResource> root,
        CriteriaBuilder builder,
        RoleAssignment roleAssignment) {
        final Classification classification = roleAssignment.getClassification();
        if (classification.equals(Classification.RESTRICTED)) {
            return builder.in(root.get(SECURITY_CLASSIFICATION)).value(
                List.of(
                    SecurityClassification.RESTRICTED,
                    SecurityClassification.PRIVATE,
                    SecurityClassification.PUBLIC
                )
            );

        } else if (classification.equals(Classification.PRIVATE)) {
            return builder.in(root.get(SECURITY_CLASSIFICATION)).value(
                List.of(
                    SecurityClassification.PRIVATE,
                    SecurityClassification.PUBLIC
                )
            );
        } else {
            return builder.in(root.get(SECURITY_CLASSIFICATION)).value(
                List.of(
                    SecurityClassification.PUBLIC
                )
            );
        }
    }

    private static Predicate mapAuthorizations(Join<TaskResource, TaskRoleResource> taskRoleResources,
                                               CriteriaBuilder builder,
                                               RoleAssignment roleAssignment) {
        Predicate nullAuthorizations = taskRoleResources.get(AUTHORIZATIONS).in((Object) new String[]{});
        if (roleAssignment.getAuthorisations() != null) {
            Predicate authorizations = taskRoleResources.get(AUTHORIZATIONS).in(
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
                return builder.equal(root.get(CASE_TYPE_ID), caseTypeValue);
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
                return builder.equal(root.get(REGION), regionVal);
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
                return builder.notEqual(root.get(CASE_ID), caseId);
            }
        }
        return builder.conjunction();
    }

    private static Predicate searchByIncludingCaseId(Root<TaskResource> root,
                                                     CriteriaBuilder builder,
                                                     RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String caseId = roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_ID.value());
            if (StringUtils.hasText(caseId)) {
                return builder.equal(root.get(CASE_ID), caseId);
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
                return builder.equal(root.get(JURISDICTION), regionVal);
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
                return builder.equal(root.get(LOCATION), regionVal);
            }
        }
        return builder.conjunction();
    }
}
