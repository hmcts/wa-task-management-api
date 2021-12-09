package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static java.util.Collections.emptyList;
import static java.util.List.of;
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


@Slf4j
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.TooManyMethods", "PMD.DataflowAnomalyAnalysis", "PMD.ExcessiveImports"})
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

    private RoleAssignmentFilter() {
        // avoid creating object
    }

    public static Specification<TaskResource> buildRoleAssignmentConstraints(
        List<PermissionTypes> permissionsRequired,
        AccessControlResponse accessControlResponse) {

        return (root, query, builder) -> {
            final Join<TaskResource, TaskRoleResource> taskRoleResources = root.join(TASK_ROLE_RESOURCES);

            // filter roles which are active.
            final List<Optional<RoleAssignment>> activeRoleAssignments = accessControlResponse.getRoleAssignments()
                .stream().map(RoleAssignmentFilter::filterByActiveRole).collect(Collectors.toList());

            // builds query for grant type BASIC, SPECIFIC
            final Predicate basicAndSpecific = RoleAssignmentFilter.buildQueryForBasicAndSpecific(
                root, taskRoleResources, builder, activeRoleAssignments);

            // builds query for grant type STANDARD, CHALLENGED
            final Predicate standardAndChallenged = RoleAssignmentFilter.buildQueryForStandardAndChallenged(
                root, taskRoleResources, builder, activeRoleAssignments);

            // builds query for grant type EXCLUDED
            final Predicate excluded = RoleAssignmentFilter.buildQueryForExcluded(
                root, builder, activeRoleAssignments);

            final Predicate standardChallengedExcluded = builder.and(standardAndChallenged, excluded.not());

            // permissions check
            List<Predicate> permissionPredicates = new ArrayList<>();
            for (PermissionTypes type : permissionsRequired) {
                permissionPredicates.add(builder.isTrue(taskRoleResources.get(type.value().toLowerCase(Locale.ROOT))));
            }
            final Predicate permissionPredicate = builder.or(permissionPredicates.toArray(new Predicate[0]));

            query.distinct(true);

            return builder.and(builder.or(basicAndSpecific, standardChallengedExcluded), permissionPredicate);
        };
    }

    public static Specification<TaskResource> buildQueryToRetrieveRoleInformation(
        AccessControlResponse accessControlResponse) {

        return (root, query, builder) -> {
            // filter roles which are active.
            final List<Optional<RoleAssignment>> activeRoleAssignments = accessControlResponse.getRoleAssignments()
                .stream().map(RoleAssignmentFilter::filterByActiveRole).collect(Collectors.toList());

            final Join<TaskResource, TaskRoleResource> taskRoleResources = root.join(TASK_ROLE_RESOURCES);

            List<Predicate> rolePredicates = new ArrayList<>();
            for (Optional<RoleAssignment> roleAssignment : activeRoleAssignments) {
                Predicate rolePredicate = builder.equal(taskRoleResources.get(ROLE_NAME_COLUMN),
                    roleAssignment.get().getRoleName());

                rolePredicates.add(builder.and(rolePredicate, builder.isTrue(taskRoleResources.get(READ_COLUMN))));
            }
            return builder.or(rolePredicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate buildQueryForBasicAndSpecific(Root<TaskResource> root,
                                                          final Join<TaskResource, TaskRoleResource> taskRoleResources,
                                                          CriteriaBuilder builder,
                                                          List<Optional<RoleAssignment>> roleAssignmentList) {

        final Set<GrantType> grantTypes = Set.of(BASIC, SPECIFIC);
        List<Predicate> rolePredicates = buildPredicates(root, taskRoleResources, builder,
                                                         roleAssignmentList, grantTypes);

        return builder.or(rolePredicates.toArray(new Predicate[0]));
    }

    private static Predicate buildQueryForStandardAndChallenged(Root<TaskResource> root,
                                                               final Join<TaskResource,
                                                               TaskRoleResource> taskRoleResources,
                                                               CriteriaBuilder builder,
                                                               List<Optional<RoleAssignment>> roleAssignmentList) {

        final Set<GrantType> grantTypes = Set.of(STANDARD, CHALLENGED);
        List<Predicate> rolePredicates = buildPredicates(root, taskRoleResources, builder,
                                                         roleAssignmentList, grantTypes);

        return builder.or(rolePredicates.toArray(new Predicate[0]));
    }

    private static Predicate buildQueryForExcluded(Root<TaskResource> root,
                                                  CriteriaBuilder builder,
                                                  List<Optional<RoleAssignment>> roleAssignmentList) {

        final Set<GrantType> grantTypes = Set.of(EXCLUDED);

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

        return rolePredicates;
    }


    private static Predicate searchByExcludedGrantType(Root<TaskResource> root,
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

        if (roleAssignment.getAttributes() != null) {
            Predicate caseTypeId = searchByCaseTypeId(root, builder, roleAssignment);
            Predicate region = searchByRegion(root, builder, roleAssignment);
            Predicate jurisdiction = searchByRoleJurisdiction(root, builder, roleAssignment);
            Predicate location = searchByRoleLocation(root, builder, roleAssignment);
            Predicate caseId = searchByIncludingCaseId(root, builder, roleAssignment);

            return builder.and(securityClassification, authorizations, jurisdiction,
                location, region, caseTypeId, caseId);
        }
        return builder.and(securityClassification, authorizations);
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
        if (classification != null) {
            switch (classification) {
                case PUBLIC:
                    return builder.in(root.get(SECURITY_CLASSIFICATION_COLUMN)).value(
                        of(
                            SecurityClassification.PUBLIC
                        )
                    );
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
        return builder.in(root.get(SECURITY_CLASSIFICATION_COLUMN)).value(
            emptyList()
        );
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
        String caseTypeValue = roleAssignment.getAttributes().get(CASE_TYPE.value());
        if (StringUtils.hasText(caseTypeValue)) {
            return builder.equal(root.get(CASE_TYPE_ID_COLUMN), caseTypeValue);
        }
        return builder.conjunction();
    }

    private static Predicate searchByRegion(Root<TaskResource> root,
                                            CriteriaBuilder builder,
                                            RoleAssignment roleAssignment) {
        String regionVal = roleAssignment.getAttributes().get(REGION.value());
        if (StringUtils.hasText(regionVal)) {
            return builder.equal(root.get(REGION_COLUMN), regionVal);
        }
        return builder.conjunction();
    }

    private static Predicate searchByIncludingCaseId(Root<TaskResource> root,
                                                     CriteriaBuilder builder,
                                                     RoleAssignment roleAssignment) {
        String caseId = roleAssignment.getAttributes().get(CASE_ID.value());
        if (StringUtils.hasText(caseId)) {
            return builder.equal(root.get(CASE_ID_COLUMN), caseId);
        }
        return builder.conjunction();
    }

    private static Predicate searchByRoleJurisdiction(Root<TaskResource> root,
                                                      CriteriaBuilder builder,
                                                      RoleAssignment roleAssignment) {
        String jurisdictionVal = roleAssignment.getAttributes().get(JURISDICTION.value());
        if (StringUtils.hasText(jurisdictionVal)) {
            return builder.equal(root.get(JURISDICTION_COLUMN), jurisdictionVal);
        }
        return builder.conjunction();
    }

    private static Predicate searchByRoleLocation(Root<TaskResource> root,
                                                  CriteriaBuilder builder,
                                                  RoleAssignment roleAssignment) {
        String locationVal = roleAssignment.getAttributes().get(BASE_LOCATION.value());
        if (StringUtils.hasText(locationVal)) {
            return builder.equal(root.get(LOCATION_COLUMN), locationVal);
        }
        return builder.conjunction();
    }

    private static Optional<RoleAssignment> filterByActiveRole(RoleAssignment roleAssignment) {
        if (hasBeginTimePermission(roleAssignment) && hasEndTimePermission(roleAssignment)) {
            return Optional.of(roleAssignment);
        }
        return Optional.empty();
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
