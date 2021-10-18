package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.TooManyMethods", "PMD.LawOfDemeter"})
public final class TaskResourceSpecification {

    public static final String TASK_ROLE_RESOURCES = "taskRoleResources";
    public static final String STATE = "state";
    public static final String LOCATION = "location";
    public static final ZoneId ZONE_ID = ZoneId.of("Europe/London");
    public static final String TASK_ID = "taskId";
    public static final String TASK_TYPE = "taskType";
    public static final String ASSIGNEE = "assignee";
    public static final String CASE_ID = "caseId";
    public static final String ROLE_NAME = "roleName";
    public static final String WORK_TYPE = "workTypeResource";

    private TaskResourceSpecification() {
        // avoid creating object
    }

    public static Specification<TaskResource> buildTaskQuery(
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired
    ) {

        return buildApplicationConstraints(searchTaskRequest)
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse));
    }

    public static Specification<TaskResource> buildSingleTaskQuery(String taskId,
                                                             AccessControlResponse accessControlResponse,
                                                             List<PermissionTypes> permissionsRequired
    ) {

        return buildRoleAssignmentConstraints(taskId,permissionsRequired, accessControlResponse);
    }

    private static Specification<TaskResource> buildRoleAssignmentConstraints(
        List<PermissionTypes> permissionsRequired,
        AccessControlResponse accessControlResponse) {

        return (root, query, builder) -> {
            final Join<TaskResource, TaskRoleResource> taskRoleResources = root.join(TASK_ROLE_RESOURCES);

            // filter roles which are active.
            final List<Optional<RoleAssignment>> activeRoleAssignments = accessControlResponse.getRoleAssignments()
                .stream().map(TaskResourceSpecification::filterByActiveRole).collect(Collectors.toList());

            // builds query for grant type BASIC, SPECIFIC
            final Predicate basicAndSpecific = RoleAssignmentFilter.buildQueryForBasicAndSpecific(
                root, taskRoleResources, builder, activeRoleAssignments);

            // builds query for grant type STANDARD, CHALLENGED
            final Predicate standardAndChallenged = RoleAssignmentFilter.buildQueryForStandardAndChallenged(
                root, taskRoleResources, builder, activeRoleAssignments);

            // builds query for grant type EXCLUDED
            final Predicate excluded = RoleAssignmentFilter.buildQueryForExcluded(
                root, taskRoleResources, builder, activeRoleAssignments);

            final Predicate standardChallengedExcluded = builder.and(standardAndChallenged, excluded.not());

            // permissions check
            List<Predicate> permissionPredicates = new ArrayList<>();
            for (PermissionTypes type : permissionsRequired) {
                permissionPredicates.add(builder.isTrue(taskRoleResources.get(type.value().toLowerCase(Locale.ROOT))));
            }
            final Predicate permissionPredicate = builder.and(permissionPredicates.toArray(new Predicate[0]));

            query.distinct(true);

            return builder.and(builder.or(basicAndSpecific, standardChallengedExcluded), permissionPredicate);
        };
    }

    private static Specification<TaskResource> buildRoleAssignmentConstraints(String taskId,
          List<PermissionTypes> permissionsRequired,
          AccessControlResponse accessControlResponse) {

        return (root, query, builder) -> {
            final Join<TaskResource, TaskRoleResource> taskRoleResources = root.join(TASK_ROLE_RESOURCES);
            final Predicate taskIdPredicate = builder.equal(root.get(TASK_ID),taskId);

            // filter roles which are active.
            final List<Optional<RoleAssignment>> activeRoleAssignments = accessControlResponse.getRoleAssignments()
                .stream().map(TaskResourceSpecification::filterByActiveRole).collect(Collectors.toList());

            // builds query for grant type BASIC, SPECIFIC
            final Predicate basicAndSpecific = RoleAssignmentFilter.buildQueryForBasicAndSpecific(
                root, taskRoleResources, builder, activeRoleAssignments);

            // builds query for grant type STANDARD, CHALLENGED
            final Predicate standardAndChallenged = RoleAssignmentFilter.buildQueryForStandardAndChallenged(
                root, taskRoleResources, builder, activeRoleAssignments);

            // builds query for grant type EXCLUDED
            final Predicate excluded = RoleAssignmentFilter.buildQueryForExcluded(
                root, taskRoleResources, builder, activeRoleAssignments);

            final Predicate standardChallengedExcluded = builder.and(standardAndChallenged, excluded.not());

            // permissions check
            List<Predicate> permissionPredicates = new ArrayList<>();
            for (PermissionTypes type : permissionsRequired) {
                permissionPredicates.add(builder.isTrue(taskRoleResources.get(type.value().toLowerCase(Locale.ROOT))));
            }
            final Predicate permissionPredicate = builder.and(permissionPredicates.toArray(new Predicate[0]));
            query.distinct(true);

            return builder.and(builder.or(basicAndSpecific, standardChallengedExcluded),
                               taskIdPredicate,permissionPredicate);

        };
    }

    private static Specification<TaskResource> buildApplicationConstraints(SearchTaskRequest searchTaskRequest) {
        return searchByJurisdiction(searchTaskRequest)
            .and(searchByState(searchTaskRequest))
            .and(searchByLocation(searchTaskRequest))
            .and(searchByCaseId(searchTaskRequest))
            .and(searchByUser(searchTaskRequest))
            .and(searchByWorkType(searchTaskRequest));
    }

    private static Specification<TaskResource> searchByState(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.STATE) != null) {
            final List<String> values = keyMap.get(SearchParameterKey.STATE).getValues();
            final List<CFTTaskState> cftTaskStates = values.stream().map(
                value -> CFTTaskState.valueOf(value.toUpperCase(Locale.ROOT))).collect(Collectors.toList());

            return (root, query, builder) -> builder.in(root.get(STATE))
                .value(cftTaskStates);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<TaskResource> searchByJurisdiction(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.JURISDICTION) != null) {
            return (root, query, builder) -> builder.in(root.get("jurisdiction"))
                .value(keyMap.get(SearchParameterKey.JURISDICTION).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<TaskResource> searchByLocation(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.LOCATION) != null) {
            final List<String> locationList = keyMap.get(SearchParameterKey.LOCATION).getValues();
            return (root, query, builder) -> builder.in(root.get(LOCATION))
                .value(locationList);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<TaskResource> searchByCaseId(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.CASE_ID) != null) {
            final List<String> caseIdList = keyMap.get(SearchParameterKey.CASE_ID).getValues();
            return (root, query, builder) -> builder.in(root.get(CASE_ID))
                .value(caseIdList);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<TaskResource> searchByUser(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.USER) != null) {
            final List<String> usersList = keyMap.get(SearchParameterKey.USER).getValues();
            return (root, query, builder) -> builder.in(root.get(ASSIGNEE))
                .value(usersList);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<TaskResource> searchByWorkType(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.WORK_TYPE) != null) {
            final List<String> workTypeList = keyMap.get(SearchParameterKey.WORK_TYPE).getValues();
            return (root, query, builder) -> builder.in(root.get(WORK_TYPE).get("id"))
                .value(workTypeList);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static EnumMap<SearchParameterKey, SearchParameter> asEnumMap(SearchTaskRequest searchTaskRequest) {
        EnumMap<SearchParameterKey, SearchParameter> map = new EnumMap<>(SearchParameterKey.class);
        if (searchTaskRequest != null && searchTaskRequest.getSearchParameters() != null) {
            searchTaskRequest.getSearchParameters()
                .forEach(request -> map.put(request.getKey(), request));
        }

        return map;
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
        return false;
    }

    private static boolean hasBeginTimePermission(RoleAssignment roleAssignment) {
        LocalDateTime beginTime = roleAssignment.getBeginTime();
        if (beginTime != null) {

            ZonedDateTime beginTimeLondonTime = beginTime.atZone(ZONE_ID);
            ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(ZONE_ID);

            return currentDateTimeLondonTime.isAfter(beginTimeLondonTime);
        }
        return false;
    }
}
