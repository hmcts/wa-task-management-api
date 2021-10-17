package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey;

import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildRoleAssignmentConstraints;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByJurisdiction;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByLocation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByState;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByUser;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.TooManyMethods", "PMD.LawOfDemeter"})
public final class TaskResourceSpecification {

    public static final String LOCATION = "location";
    public static final String TASK_ID = "taskId";
    public static final String TASK_TYPE = "taskType";
    public static final String CASE_ID = "caseId";

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
        return searchByTaskId(taskId)
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse));
    }


    public static Specification<TaskResource> buildQueryForCompletable(
        SearchEventAndCase searchEventAndCase, AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired, List<String> taskTypes) {

        return searchByCaseId(List.of(searchEventAndCase.getCaseId()))
            .and(searchByState(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .and(searchByTaskTypes(taskTypes))
            .and(searchByUser(List.of(accessControlResponse.getUserInfo().getUid())))
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse));
    }

    private static Specification<TaskResource> buildApplicationConstraints(SearchTaskRequest searchTaskRequest) {
        return extractJurisdiction(searchTaskRequest)
            .and(extractState(searchTaskRequest))
            .and(extractLocation(searchTaskRequest))
            .and(extractCaseId(searchTaskRequest))
            .and(extractUser(searchTaskRequest));
    }

    private static Specification<TaskResource> extractState(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.STATE) != null) {
            final List<String> values = keyMap.get(SearchParameterKey.STATE).getValues();
            final List<CFTTaskState> cftTaskStates = values.stream().map(CFTTaskState::valueOf)
                .collect(Collectors.toList());

            return searchByState(cftTaskStates);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<TaskResource> extractJurisdiction(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.JURISDICTION) != null) {
            return searchByJurisdiction(keyMap.get(SearchParameterKey.JURISDICTION).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<TaskResource> extractLocation(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.LOCATION) != null) {
            return searchByLocation(keyMap.get(SearchParameterKey.LOCATION).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<TaskResource> extractCaseId(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.CASE_ID) != null) {
            return searchByCaseId(keyMap.get(SearchParameterKey.CASE_ID).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<TaskResource> extractUser(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.USER) != null) {
            return searchByUser(keyMap.get(SearchParameterKey.USER).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    private static Specification<TaskResource> searchByTaskId(String taskId) {
        return (root, query, builder) -> builder.equal(root.get(TASK_ID), taskId);
    }

    private static Specification<TaskResource> searchByTaskTypes(List<String> taskTypes) {
        return (root, query, builder) -> builder.in(root.get(TASK_TYPE))
            .value(taskTypes);
    }

    private static EnumMap<SearchParameterKey, SearchParameter> asEnumMap(SearchTaskRequest searchTaskRequest) {
        EnumMap<SearchParameterKey, SearchParameter> map = new EnumMap<>(SearchParameterKey.class);
        if (searchTaskRequest != null && searchTaskRequest.getSearchParameters() != null) {
            searchTaskRequest.getSearchParameters()
                .forEach(request -> map.put(request.getKey(), request));
        }

        return map;
    }

}
