package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.BASE_LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.BASIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.CHALLENGED;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.EXCLUDED;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.SPECIFIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.STANDARD;


@SuppressWarnings({"PMD.LawOfDemeter", "PMD.TooManyMethods", "PMD.DataflowAnomalyAnalysis"})
public final class RoleAssignmentFilter {

    public static final String CASE_ID_COLUMN = "caseId";
    public static final String JURISDICTION_COLUMN = "jurisdiction";
    public static final String LOCATION_COLUMN = "location";
    public static final String REGION_COLUMN = "region";
    public static final String CASE_TYPE_ID_COLUMN = "caseTypeId";
    public static final String AUTHORIZATIONS_COLUMN = "authorizations";
    public static final String SECURITY_CLASSIFICATION_COLUMN = "securityClassification";
    public static final String ROLE_NAME_COLUMN = "roleName";

    private RoleAssignmentFilter() {
        // avoid creating object
    }

    public static Predicate buildQueryForBasicAndSpecific(Root<TaskResource> root,
                                                          final Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                          CriteriaBuilder builder,
                                                          List<Optional<RoleAssignment>> roleAssignmentList) {

        final Set<GrantType> grantTypes = Set.of(BASIC, SPECIFIC);
        List<Predicate> rolePredicates = buildPredicates(root, taskRoleResources, builder,
            roleAssignmentList, grantTypes);

        return builder.or(rolePredicates.toArray(new Predicate[0]));
    }

    public static Predicate buildQueryForStandardAndChallenged(Root<TaskResource> root,
                                                               final Join<TaskResource,
                                                                   TaskRoleResource> taskRoleResources,
                                                               CriteriaBuilder builder,
                                                               List<Optional<RoleAssignment>> roleAssignmentList) {

        final Set<GrantType> grantTypes = Set.of(STANDARD, CHALLENGED);
        List<Predicate> rolePredicates = buildPredicates(root, taskRoleResources, builder,
            roleAssignmentList, grantTypes);

        return builder.or(rolePredicates.toArray(new Predicate[0]));
    }

    public static Predicate buildQueryForExcluded(Root<TaskResource> root,
                                                  final Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                  CriteriaBuilder builder,
                                                  List<Optional<RoleAssignment>> roleAssignmentList) {

        final Set<GrantType> grantTypes = Set.of(EXCLUDED);
        //todo: can't cover
        final Set<RoleAssignment> roleAssignmentsForGrantTypes = roleAssignmentList.stream()
            .flatMap(Optional::stream).filter(ra -> grantTypes.contains(ra.getGrantType())).collect(Collectors.toSet());
        List<Predicate> rolePredicates = new ArrayList<>();
        for (RoleAssignment roleAssignment : roleAssignmentsForGrantTypes) {
            rolePredicates.add(searchByExcludedGrantType(root, builder, roleAssignment));
        }

        return builder.or(rolePredicates.toArray(new Predicate[0]));
    }

    private static List<Predicate> buildPredicates(Root<TaskResource> root, Join<TaskResource,
        TaskRoleResource> taskRoleResources, CriteriaBuilder builder, List<Optional<RoleAssignment>> roleAssignmentList,
                                                   Set<GrantType> grantTypes) {
        //todo: can't cover
        final Set<RoleAssignment> roleAssignmentsForGrantTypes = roleAssignmentList.stream()
            .flatMap(Optional::stream).filter(ra -> grantTypes.contains(ra.getGrantType())).collect(Collectors.toSet());

        List<Predicate> rolePredicates = new ArrayList<>();
        for (RoleAssignment roleAssignment : roleAssignmentsForGrantTypes) {
            Predicate roleName = builder.equal(taskRoleResources.get(ROLE_NAME_COLUMN),
                roleAssignment.getRoleName());
            final Predicate mandatoryPredicates = buildMandatoryPredicates(root, taskRoleResources,
                builder, roleAssignment);
            rolePredicates.add(builder.and(roleName, mandatoryPredicates));
        }

        //todo: empty list doesn't check in tests  return Collections.emptyList();
        return rolePredicates;
    }


    public static Predicate searchByExcludedGrantType(Root<TaskResource> root,
                                                      CriteriaBuilder builder,
                                                      RoleAssignment roleAssignment) {
        Predicate securityClassification = mapSecurityClassification(
            root, builder, roleAssignment
        );
        Predicate caseId = searchByIncludingCaseId(root, builder, roleAssignment);

        return builder.and(securityClassification, caseId);
    }

    private static Predicate buildMandatoryPredicates(Root<TaskResource> root,
                                                      Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                      CriteriaBuilder builder,
                                                      RoleAssignment roleAssignment) {
        Predicate securityClassification = mapSecurityClassification(
            root, builder, roleAssignment
        );
        Predicate authorizations;
        if (CHALLENGED.equals(roleAssignment.getGrantType())) {
            authorizations = mapAuthorizations(taskRoleResources, builder, roleAssignment);
        } else {
            authorizations = getEmptyOrNullAuthorizationsPredicate(taskRoleResources, builder);
        }

        Predicate caseTypeId = searchByCaseTypeId(root, builder, roleAssignment);
        Predicate region = searchByRegion(root, builder, roleAssignment);
        Predicate jurisdiction = searchByRoleJurisdiction(root, builder, roleAssignment);
        Predicate location = searchByRoleLocation(root, builder, roleAssignment);
        Predicate caseId = searchByIncludingCaseId(root, builder, roleAssignment);

        return builder.and(securityClassification, authorizations, jurisdiction,
            location, region, caseTypeId, caseId);
    }

    private static Predicate getEmptyOrNullAuthorizationsPredicate(
        Join<TaskResource, TaskRoleResource> taskRoleResources, CriteriaBuilder builder
    ) {
        Predicate nullAuthorizations = taskRoleResources.get(AUTHORIZATIONS_COLUMN).isNull();
        Predicate emptyAuthorizations = builder.equal(
            taskRoleResources.get(AUTHORIZATIONS_COLUMN), new String[]{}
        );
        return builder.or(nullAuthorizations, emptyAuthorizations);
    }

    private static Predicate mapSecurityClassification(
        Root<TaskResource> root,
        CriteriaBuilder builder,
        RoleAssignment roleAssignment) {
        final Classification classification = roleAssignment.getClassification();
        if (classification.equals(Classification.RESTRICTED)) {
            return builder.in(root.get(SECURITY_CLASSIFICATION_COLUMN)).value(
                List.of(
                    SecurityClassification.RESTRICTED,
                    SecurityClassification.PRIVATE,
                    SecurityClassification.PUBLIC
                )
            );

        } else if (classification.equals(Classification.PRIVATE)) {
            return builder.in(root.get(SECURITY_CLASSIFICATION_COLUMN)).value(
                List.of(
                    SecurityClassification.PRIVATE,
                    SecurityClassification.PUBLIC
                )
            );
        } else {
            return builder.in(root.get(SECURITY_CLASSIFICATION_COLUMN)).value(
                List.of(
                    SecurityClassification.PUBLIC
                )
            );
        }
    }

    private static Predicate mapAuthorizations(Join<TaskResource, TaskRoleResource> taskRoleResources,
                                               CriteriaBuilder builder,
                                               RoleAssignment roleAssignment) {
        Predicate nullAuthorizations = getEmptyOrNullAuthorizationsPredicate(taskRoleResources, builder);
        if (roleAssignment.getAuthorisations() != null) {
            Predicate authorizations = taskRoleResources.get(AUTHORIZATIONS_COLUMN).in(
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
            String caseTypeValue = roleAssignment.getAttributes().get(CASE_TYPE.value());
            if (StringUtils.hasText(caseTypeValue)) {
                return builder.equal(root.get(CASE_TYPE_ID_COLUMN), caseTypeValue);
            }
        }
        return builder.conjunction();
    }

    private static Predicate searchByRegion(Root<TaskResource> root,
                                            CriteriaBuilder builder,
                                            RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String regionVal = roleAssignment.getAttributes().get(REGION.value());
            if (StringUtils.hasText(regionVal)) {
                return builder.equal(root.get(REGION_COLUMN), regionVal);
            }
        }
        return builder.conjunction();
    }

    private static Predicate searchByIncludingCaseId(Root<TaskResource> root,
                                                     CriteriaBuilder builder,
                                                     RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String caseId = roleAssignment.getAttributes().get(CASE_ID.value());
            if (StringUtils.hasText(caseId)) {
                return builder.equal(root.get(CASE_ID_COLUMN), caseId);
            }
        }
        return builder.conjunction();
    }

    private static Predicate searchByRoleJurisdiction(Root<TaskResource> root,
                                                      CriteriaBuilder builder,
                                                      RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String jurisdictionVal = roleAssignment.getAttributes().get(JURISDICTION.value());
            if (StringUtils.hasText(jurisdictionVal)) {
                return builder.equal(root.get(JURISDICTION_COLUMN), jurisdictionVal);
            }
        }
        return builder.conjunction();
    }

    private static Predicate searchByRoleLocation(Root<TaskResource> root,
                                                  CriteriaBuilder builder,
                                                  RoleAssignment roleAssignment) {
        if (roleAssignment.getAttributes() != null) {
            String locationVal = roleAssignment.getAttributes().get(BASE_LOCATION.value());
            if (StringUtils.hasText(locationVal)) {
                return builder.equal(root.get(LOCATION_COLUMN), locationVal);
            }
        }
        return builder.conjunction();
    }
}
