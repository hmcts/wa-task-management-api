package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
public final class TaskResourceSpecification {

    private TaskResourceSpecification(){
        // avoid creating object
    }

    public static Specification<TaskResource> getTasks(
            SearchTaskRequest searchTaskRequest,
            AccessControlResponse accessControlResponse,
            List<PermissionTypes> permissionsRequired
    ) {

        return searchByRoles(permissionsRequired, accessControlResponse)
            .and(searchByClassification(accessControlResponse)
            .and(searchByRegion()
            .and(searchByState(searchTaskRequest)
            .and(searchByLocation(searchTaskRequest)
            .and(searchByJurisdiction(searchTaskRequest)
            .and(searchByCaseId(searchTaskRequest)
            .and(searchByUser(searchTaskRequest))))))));
    }

    public static Specification<TaskResource> searchByRoles(List<PermissionTypes> permissionsRequired,
                                                            AccessControlResponse accessControlResponse) {
        return (root, query, builder) -> {
            final Join<TaskResource, TaskRoleResource> taskRoleResources = root.join("taskRoleResources");
            List<Predicate> rolePredicates = new ArrayList<>();
            for (RoleAssignment roleAssignment : accessControlResponse.getRoleAssignments()) {
                rolePredicates.add(builder.equal(taskRoleResources.get("roleName"), roleAssignment.getRoleName()));
            }
            List<Predicate> permissionPredicates = new ArrayList<>();
            for (PermissionTypes type : permissionsRequired) {
                permissionPredicates.add(builder.isTrue(taskRoleResources.get(type.value().toLowerCase(Locale.ROOT))));
            }
            final Predicate rolePredicate = builder.or(rolePredicates.toArray(new Predicate[0]));
            final Predicate permissionPredicate = builder.and(permissionPredicates.toArray(new Predicate[0]));

            query.distinct(true);
            return builder.and(rolePredicate, permissionPredicate);
        };
    }

    public static Specification<TaskResource> searchByClassification(AccessControlResponse accessControlResponse) {
        for (RoleAssignment roleAssignment : accessControlResponse.getRoleAssignments()) {
            final Classification classification = roleAssignment.getClassification();
            if (classification.equals(Classification.RESTRICTED)) {
                return (root, query, builder) -> builder.in(root.get("securityClassification")).value(
                    List.of(
                        SecurityClassification.RESTRICTED,
                        SecurityClassification.PRIVATE,
                        SecurityClassification.PUBLIC
                    )
                );
            } else if (classification.equals(Classification.PRIVATE)) {
                return (root, query, builder) -> builder.in(root.get("securityClassification")).value(
                    List.of(SecurityClassification.PRIVATE, SecurityClassification.PUBLIC));
            } else {
                return (root, query, builder) -> builder.in(root.get("securityClassification")).value(
                    SecurityClassification.PUBLIC
                );
            }
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    public static Specification<TaskResource> searchByState(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.STATE) != null) {
            final List<String> values = keyMap.get(SearchParameterKey.STATE).getValues();
            final List<CFTTaskState> cftTaskStates = values.stream().map(CFTTaskState::valueOf)
                .collect(Collectors.toList());

            return (root, query, builder) -> builder.in(root.get("state"))
                .value(cftTaskStates);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByJurisdiction(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.JURISDICTION) != null) {
            return (root, query, builder) -> builder.in(root.get("jurisdiction"))
                .value(keyMap.get(SearchParameterKey.JURISDICTION).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByLocation(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.LOCATION) != null) {
            final List<String> locationList = keyMap.get(SearchParameterKey.LOCATION).getValues();
            return (root, query, builder) -> builder.in(root.get("location"))
                .value(locationList);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByCaseId(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.CASE_ID) != null) {
            final List<String> caseIdList = keyMap.get(SearchParameterKey.CASE_ID).getValues();
            return (root, query, builder) -> builder.in(root.get("caseId"))
                .value(caseIdList);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByUser(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.USER) != null) {
            final List<String> usersList = keyMap.get(SearchParameterKey.USER).getValues();
            return (root, query, builder) -> builder.in(root.get("assignee"))
                .value(usersList);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByRegion() {
        return (root, query, builder) -> builder.equal(root.get("region"), "1");
    }


    private static EnumMap<SearchParameterKey, SearchParameter> asEnumMap(SearchTaskRequest searchTaskRequest) {
        EnumMap<SearchParameterKey, SearchParameter> map = new EnumMap<>(SearchParameterKey.class);
        searchTaskRequest.getSearchParameters()
            .forEach(request -> map.put(request.getKey(), request));

        return map;
    }

}
