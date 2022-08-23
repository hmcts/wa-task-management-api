package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirement;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentForSearch;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static java.util.Collections.emptyList;
import static java.util.List.of;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.CHALLENGED;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.EXCLUDED;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.SPECIFIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.STANDARD;


@Slf4j
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.TooManyMethods", "PMD.DataflowAnomalyAnalysis",
    "PMD.ExcessiveImports", "PMD.GodClass"})
public final class RoleAssignmentFilter {

    public static final String CASE_ID_COLUMN = "caseId";
    public static final String JURISDICTION_COLUMN = "jurisdiction";
    public static final String LOCATION_COLUMN = "location";
    public static final String REGION_COLUMN = "region";
    public static final String CASE_TYPE_ID_COLUMN = "caseTypeId";
    public static final String AUTHORIZATIONS_COLUMN = "authorizations";
    public static final String SECURITY_CLASSIFICATION_COLUMN = "securityClassification";
    public static final String ROLE_NAME_COLUMN = "roleName";
    public static final String TASK_ROLE_RESOURCES = "taskRoleResources";
    public static final ZoneId ZONE_ID = ZoneId.of("Europe/London");
    public static final String READ_COLUMN = "read";
    public static final int ONE = 1;

    private RoleAssignmentFilter() {
        // avoid creating object
    }

    public static Predicate buildRoleAssignmentConstraints(
        List<PermissionTypes> permissionsRequired,
        List<RoleAssignment> roleAssignments,
        boolean andPermissions,
        CriteriaBuilder builder,
        Root<TaskResource> root) {

        final Join<TaskResource, TaskRoleResource> taskRoleResources = root.join(TASK_ROLE_RESOURCES);

        // roll assigment filter
        Predicate roleAssignmentFilterPredicate = getRoleAssignmentFilterPredicate(roleAssignments,
                                                                                   builder, root, taskRoleResources);

        // permissions check
        Predicate permissionRequirementPredicate = getPermissionRequirementPredicate(permissionsRequired,
                                                                                     builder, taskRoleResources,
                                                                                     andPermissions);

        return builder.and(roleAssignmentFilterPredicate, permissionRequirementPredicate);
    }

    public static Predicate buildRoleAssignmentConstraints(
        PermissionRequirements permissionsRequired,
        List<RoleAssignment> roleAssignments,
        CriteriaBuilder builder,
        Root<TaskResource> root) {

        final Join<TaskResource, TaskRoleResource> taskRoleResources = root.join(TASK_ROLE_RESOURCES);

        // roll assigment filter
        Predicate roleAssignmentFilterPredicate = getRoleAssignmentFilterPredicate(roleAssignments,
                                                                                   builder, root, taskRoleResources);

        // permissions check
        Predicate permissionRequirementPredicate = getPermissionRequirementPredicate(permissionsRequired,
                                                                                     builder, taskRoleResources);

        return builder.and(roleAssignmentFilterPredicate, permissionRequirementPredicate);
    }

    public static Predicate buildQueryToRetrieveRoleInformation(
        List<RoleAssignment> roleAssignments,
        CriteriaBuilder builder,
        Root<TaskResource> root) {
        // filter roles which are active.
        final List<RoleAssignment> activeRoleAssignments = roleAssignments
            .stream().filter(RoleAssignmentFilter::filterByActiveRole).collect(Collectors.toList());

        RoleAssignmentSearchData searchData = new RoleAssignmentSearchData(
            activeRoleAssignments,
            RoleAssignmentForSearch::getRoleType
        );
        List<RoleAssignmentForSearch> roleAssignmentsForSearch = searchData.getRoleAssignmentsForSearch();

        final Join<TaskResource, TaskRoleResource> taskRoleResources = root.join(TASK_ROLE_RESOURCES);

        List<Predicate> rolePredicates = new ArrayList<>();
        for (RoleAssignmentForSearch roleAssignment : roleAssignmentsForSearch) {

            Predicate rolePredicate = builder.equal(
                taskRoleResources.get(ROLE_NAME_COLUMN),
                roleAssignment.getRoleName()
            );

            rolePredicates.add(builder.and(rolePredicate, builder.isTrue(taskRoleResources.get(READ_COLUMN))));

        }
        return builder.or(rolePredicates.toArray(new Predicate[0]));
    }

    private static Predicate getRoleAssignmentFilterPredicate(List<RoleAssignment> roleAssignments,
                                                              CriteriaBuilder builder,
                                                              Root<TaskResource> root,
                                                              Join<TaskResource, TaskRoleResource> taskRoleResources) {
        final List<RoleAssignment> activeRoleAssignments = roleAssignments
            .stream().filter(RoleAssignmentFilter::filterByActiveRole).collect(Collectors.toList());

        RoleAssignmentSearchData searchData = new RoleAssignmentSearchData(
            activeRoleAssignments,
            RoleAssignmentForSearch::getRoleType
        );
        List<RoleAssignmentForSearch> roleAssignmentsForSearch = searchData.getRoleAssignmentsForSearch();

        // builds query for grant type SPECIFIC
        final Predicate specific = RoleAssignmentFilter.buildQueryForSpecific(
            root, taskRoleResources, builder, roleAssignmentsForSearch);

        // builds query for grant type STANDARD, CHALLENGED
        final Predicate standardAndChallenged = RoleAssignmentFilter.buildQueryForStandardAndChallenged(
            root, taskRoleResources, builder, roleAssignmentsForSearch);

        // builds query for grant type EXCLUDED
        final Predicate excluded = RoleAssignmentFilter.buildQueryForExcluded(
            root, builder, roleAssignmentsForSearch);

        final Predicate standardChallengedExcluded = builder.and(standardAndChallenged, excluded.not());

        return builder.or(specific, standardChallengedExcluded);
    }

    private static Predicate getPermissionRequirementPredicate(List<PermissionTypes> permissionsRequired,
                                                               CriteriaBuilder builder,
                                                               Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                               boolean andPermissions) {
        List<Predicate> permissionPredicates = new ArrayList<>();
        for (PermissionTypes type : permissionsRequired) {
            permissionPredicates.add(builder.isTrue(taskRoleResources.get(type.value().toLowerCase(Locale.ROOT))));
        }
        Predicate permissionPredicate;
        if (andPermissions) {
            permissionPredicate = builder.and(permissionPredicates.toArray(new Predicate[0]));
        } else {
            permissionPredicate = builder.or(permissionPredicates.toArray(new Predicate[0]));
        }

        return permissionPredicate;
    }

    private static Predicate getPermissionRequirementPredicate(PermissionRequirements permissionsRequired,
                                                               CriteriaBuilder builder,
                                                               Join<TaskResource, TaskRoleResource> taskRoleResources) {
        PermissionRequirements nextRequirements = permissionsRequired;
        PermissionJoin nextPermissionJoin = nextRequirements.getPermissionJoin();
        Predicate permissionRequirementPredicate = null;

        List<Predicate> permissionPredicates = new ArrayList<>();
        Predicate[] predicates = new Predicate[0];

        while (nextRequirements != null && !nextRequirements.isEmpty()) {
            PermissionRequirement requirement = nextRequirements.getPermissionRequirement();

            for (PermissionTypes type : requirement.getPermissionTypes()) {
                permissionPredicates.add(builder.isTrue(taskRoleResources.get(type.value().toLowerCase(Locale.ROOT))));
            }
            Predicate permissionPredicate;
            if (PermissionJoin.AND.equals(requirement.getPermissionJoin())) {
                permissionPredicate = builder.and(permissionPredicates.toArray(predicates));
            } else {
                permissionPredicate = builder.or(permissionPredicates.toArray(predicates));
            }
            permissionPredicates.clear();

            if (permissionRequirementPredicate == null) {
                permissionRequirementPredicate = permissionPredicate;
            } else if (PermissionJoin.AND.equals(nextPermissionJoin)) {
                permissionRequirementPredicate = builder.and(permissionRequirementPredicate, permissionPredicate);
            } else {
                permissionRequirementPredicate = builder.or(permissionRequirementPredicate, permissionPredicate);
            }

            nextPermissionJoin = nextRequirements.getPermissionJoin();
            nextRequirements = nextRequirements.getNextPermissionRequirements();
        }
        return permissionRequirementPredicate;
    }

    private static Predicate buildQueryForSpecific(Root<TaskResource> root,
                                                   final Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                   CriteriaBuilder builder,
                                                   List<RoleAssignmentForSearch> roleAssignmentList) {

        final Set<GrantType> grantTypes = Set.of(SPECIFIC);
        List<Predicate> rolePredicates = buildPredicates(root, taskRoleResources, builder,
                                                         roleAssignmentList, grantTypes
        );

        return builder.or(rolePredicates.toArray(new Predicate[0]));
    }

    private static Predicate buildQueryForStandardAndChallenged(Root<TaskResource> root,
                                                                final Join<TaskResource,
                                                                    TaskRoleResource> taskRoleResources,
                                                                CriteriaBuilder builder,
                                                                List<RoleAssignmentForSearch> roleAssignmentList) {

        final Set<GrantType> grantTypes = Set.of(STANDARD, CHALLENGED);
        List<Predicate> rolePredicates = buildPredicates(root, taskRoleResources, builder,
                                                         roleAssignmentList, grantTypes
        );

        return builder.or(rolePredicates.toArray(new Predicate[0]));
    }

    private static Predicate buildQueryForExcluded(Root<TaskResource> root,
                                                   CriteriaBuilder builder,
                                                   List<RoleAssignmentForSearch> roleAssignmentList) {

        final Set<GrantType> grantTypes = Set.of(EXCLUDED);

        final Set<RoleAssignmentForSearch> roleAssignmentsForGrantTypes = roleAssignmentList.stream()
            .filter(ra -> grantTypes.contains(GrantType.valueOf(ra.getGrantType()))).collect(Collectors.toSet());

        List<Predicate> rolePredicates = new ArrayList<>();
        for (RoleAssignmentForSearch roleAssignment : roleAssignmentsForGrantTypes) {
            rolePredicates.add(searchByExcludedGrantType(root, builder, roleAssignment));
        }

        return builder.or(rolePredicates.toArray(new Predicate[0]));
    }

    private static List<Predicate> buildPredicates(Root<TaskResource> root,
                                                   Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                   CriteriaBuilder builder,
                                                   List<RoleAssignmentForSearch> roleAssignmentList,
                                                   //grouped Role Assignment Map for the required Grant Types e.g.
                                                   // Specific, Basic
                                                   Set<GrantType> grantTypes) {

        final Set<RoleAssignmentForSearch> roleAssignmentsForGrantTypes =
            roleAssignmentList.stream()
                .filter(ra -> grantTypes.contains(GrantType.valueOf(ra.getGrantType())))
                .collect(Collectors.toSet());

        List<Predicate> rolePredicates = new ArrayList<>();

        for (RoleAssignmentForSearch roleAssignment : roleAssignmentsForGrantTypes) {
            Predicate roleName = builder.equal(
                taskRoleResources.get(ROLE_NAME_COLUMN),
                roleAssignment.getRoleName()
            );
            final Predicate mandatoryPredicates = buildMandatoryPredicates(root, taskRoleResources,
                                                                           builder, roleAssignment
            );
            rolePredicates.add(builder.and(roleName, mandatoryPredicates));
        }

        return rolePredicates;
    }


    private static Predicate searchByExcludedGrantType(Root<TaskResource> root,
                                                       CriteriaBuilder builder,
                                                       RoleAssignmentForSearch roleAssignment) {
        Predicate securityClassification = mapSecurityClassification(
            root, builder, roleAssignment
        );
        Predicate caseId = searchByIncludingCaseId(root, builder, roleAssignment);

        return builder.and(securityClassification, caseId);
    }

    private static Predicate buildMandatoryPredicates(Root<TaskResource> root,
                                                      Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                      CriteriaBuilder builder,
                                                      RoleAssignmentForSearch roleAssignment) {
        Predicate securityClassification = mapSecurityClassification(
            root, builder, roleAssignment
        );
        Predicate authorizations = mapAuthorizations(taskRoleResources, builder, roleAssignment);

        Predicate caseTypeId = searchByCaseTypeId(root, builder, roleAssignment);
        Predicate region = searchByRegion(root, builder, roleAssignment);
        Predicate jurisdiction = searchByRoleJurisdiction(root, builder, roleAssignment);
        Predicate location = searchByRoleLocation(root, builder, roleAssignment);
        Predicate caseId = searchByIncludingCaseId(root, builder, roleAssignment);

        return builder.and(securityClassification, authorizations, jurisdiction,
                           location, region, caseTypeId, caseId
        );
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
        RoleAssignmentForSearch roleAssignment) {
        final Classification classification = Classification.valueOf(roleAssignment.getClassification());
        switch (classification) {
            case PUBLIC:
                return builder.equal(root.get(SECURITY_CLASSIFICATION_COLUMN), SecurityClassification.PUBLIC);
            case PRIVATE:
                return builder.in(root.get(SECURITY_CLASSIFICATION_COLUMN)).value(
                    of(
                        SecurityClassification.PRIVATE,
                        SecurityClassification.PUBLIC
                    )
                );
            case RESTRICTED:
                return builder.in(root.get(SECURITY_CLASSIFICATION_COLUMN)).value(
                    of(
                        SecurityClassification.RESTRICTED,
                        SecurityClassification.PRIVATE,
                        SecurityClassification.PUBLIC
                    )
                );
            default:
                return builder.in(root.get(SECURITY_CLASSIFICATION_COLUMN)).value(
                    emptyList()
                );
        }
    }

    private static Predicate mapAuthorizations(Join<TaskResource, TaskRoleResource> taskRoleResources,
                                               CriteriaBuilder builder,
                                               RoleAssignmentForSearch roleAssignment) {
        Predicate nullAuthorizations = getEmptyOrNullAuthorizationsPredicate(taskRoleResources, builder);
        if (roleAssignment.getAuthorisations() != null) {
            if (roleAssignment.getAuthorisations().isEmpty()) {
                Predicate authorizations = taskRoleResources.get(AUTHORIZATIONS_COLUMN).in(
                    (Object) roleAssignment.getAuthorisations().toArray()
                );
                return builder.or(nullAuthorizations, authorizations);

            }
            return builder.or(nullAuthorizations, builder.isTrue(builder.function(
                "contains_text",
                Boolean.class,
                taskRoleResources.get(AUTHORIZATIONS_COLUMN),
                builder.literal(
                    roleAssignment.getAuthorisations().toString().replace("[", "{").replace("]", "}")
                )
            )));
        }
        return nullAuthorizations;
    }

    private static Predicate searchByCaseTypeId(Root<TaskResource> root,
                                                CriteriaBuilder builder,
                                                RoleAssignmentForSearch roleAssignment) {
        String caseTypeValue = roleAssignment.getCaseType();
        if (StringUtils.hasText(caseTypeValue)) {
            return builder.equal(root.get(CASE_TYPE_ID_COLUMN), caseTypeValue);
        }
        return builder.conjunction();
    }

    private static Predicate searchByRegion(Root<TaskResource> root,
                                            CriteriaBuilder builder,
                                            RoleAssignmentForSearch roleAssignment) {
        String regionVal = roleAssignment.getRegion();
        if (StringUtils.hasText(regionVal)) {
            return builder.equal(root.get(REGION_COLUMN), regionVal);
        }
        return builder.conjunction();
    }

    private static Predicate searchByIncludingCaseId(Root<TaskResource> root,
                                                     CriteriaBuilder builder,
                                                     RoleAssignmentForSearch roleAssignment) {
        Set<String> caseIds = roleAssignment.getCaseIds();
        if (!CollectionUtils.isEmpty(caseIds)) {
            if (caseIds.size() == ONE) {
                return builder.equal(root.get(CASE_ID_COLUMN), caseIds.iterator().next());
            }
            return builder.in(root.get(CASE_ID_COLUMN)).value(caseIds);
        }
        return builder.conjunction();
    }

    private static Predicate searchByRoleJurisdiction(Root<TaskResource> root,
                                                      CriteriaBuilder builder,
                                                      RoleAssignmentForSearch roleAssignment) {
        String jurisdictionVal = roleAssignment.getJurisdiction();
        if (StringUtils.hasText(jurisdictionVal)) {
            return builder.equal(root.get(JURISDICTION_COLUMN), jurisdictionVal);
        }
        return builder.conjunction();
    }

    private static Predicate searchByRoleLocation(Root<TaskResource> root,
                                                  CriteriaBuilder builder,
                                                  RoleAssignmentForSearch roleAssignment) {
        String locationVal = roleAssignment.getLocation();
        if (StringUtils.hasText(locationVal)) {
            return builder.equal(root.get(LOCATION_COLUMN), locationVal);
        }
        return builder.conjunction();
    }

    private static boolean filterByActiveRole(RoleAssignment roleAssignment) {
        return hasBeginTimePermission(roleAssignment) && hasEndTimePermission(roleAssignment);
    }

    private static boolean hasEndTimePermission(RoleAssignment roleAssignment) {
        LocalDateTime endTime = roleAssignment.getEndTime();
        if (endTime != null) {

            ZonedDateTime endTimeLondonTime = endTime.atZone(ZONE_ID);
            ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(ZONE_ID);

            return currentDateTimeLondonTime.isBefore(endTimeLondonTime);
        }
        return true;
    }

    private static boolean hasBeginTimePermission(RoleAssignment roleAssignment) {
        LocalDateTime beginTime = roleAssignment.getBeginTime();
        if (beginTime != null) {

            ZonedDateTime beginTimeLondonTime = beginTime.atZone(ZONE_ID);
            ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(ZONE_ID);

            return currentDateTimeLondonTime.isAfter(beginTimeLondonTime);
        }
        return true;
    }
}
