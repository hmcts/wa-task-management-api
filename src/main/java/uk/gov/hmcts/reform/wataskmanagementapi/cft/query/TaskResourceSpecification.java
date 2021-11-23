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

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildQueryToRetrieveRoleInformation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildRoleAssignmentConstraints;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByJurisdiction;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByLocation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByState;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskTypes;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByUser;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByWorkType;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.TooManyMethods", "PMD.LawOfDemeter"})
public final class TaskResourceSpecification {

    public static final String LOCATION = "location";
    public static final String TASK_ID = "taskId";
    public static final String TASK_TYPE = "taskType";
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
        return searchByTaskId(taskId)
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse));
    }

    public static Specification<TaskResource> buildTaskRolePermissionsQuery(
        String taskId,
        AccessControlResponse accessControlResponse) {

        return searchByTaskId(taskId)
            .and(buildQueryToRetrieveRoleInformation(accessControlResponse));
    }

    public static Specification<TaskResource> buildQueryForCompletable(
        SearchEventAndCase searchEventAndCase, AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired, List<String> taskTypes) {

        final String caseId = searchEventAndCase.getCaseId();
        List<String> caseIdList = caseId == null ? Collections.emptyList() : List.of(caseId);
        return searchByCaseId(caseIdList)
            .and(searchByState(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .and(searchByTaskTypes(taskTypes))
            //.and(searchByUser(List.of(accessControlResponse.getUserInfo().getUid())))
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse));
    }

    private static Specification<TaskResource> buildApplicationConstraints(SearchTaskRequest searchTaskRequest) {
        return extractJurisdiction(searchTaskRequest)
            .and(extractState(searchTaskRequest))
            .and(extractLocation(searchTaskRequest))
            .and(extractCaseId(searchTaskRequest))
            .and(extractUser(searchTaskRequest))
            .and(extractWorkType(searchTaskRequest));
    }

    private static Specification<TaskResource> extractState(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.STATE) != null) {
            final List<String> values = keyMap.get(SearchParameterKey.STATE).getValues();
            final List<CFTTaskState> cftTaskStates = values.stream().map(
                value -> CFTTaskState.valueOf(value.toUpperCase(Locale.ROOT))).collect(Collectors.toList());

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

    private static Specification<TaskResource> extractWorkType(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.WORK_TYPE) != null) {
            return searchByWorkType(keyMap.get(SearchParameterKey.WORK_TYPE).getValues());
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

}
