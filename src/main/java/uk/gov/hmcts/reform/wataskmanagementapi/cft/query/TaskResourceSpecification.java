package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildQueryToRetrieveRoleInformation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildRoleAssignmentConstraints;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByJurisdiction;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByLocation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByRoleCategory;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByState;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskTypes;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByUser;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByWorkType;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.WORK_TYPE;

@SuppressWarnings({
        "PMD.DataflowAnomalyAnalysis",
        "PMD.TooManyMethods",
        "PMD.LawOfDemeter",
        "PMD.ExcessiveImports",
    })
public final class TaskResourceSpecification {

    private TaskResourceSpecification() {
        // avoid creating object
    }

    public static Specification<TaskResource> buildTaskQuery(
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired
    ) {

        return buildApplicationConstraints(searchTaskRequest, permissionsRequired)
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

        return searchByCaseId(searchEventAndCase.getCaseId())
            .and(searchByState(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .and(searchByTaskTypes(taskTypes))
            //.and(searchByUser(List.of(accessControlResponse.getUserInfo().getUid())))
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse));
    }

    private static Specification<TaskResource> buildApplicationConstraints(
        SearchTaskRequest searchTaskRequest, List<PermissionTypes> permissionsRequired) {

        final EnumMap<SearchParameterKey, SearchParameterList> keyMap = asEnumMapForListOfStrings(searchTaskRequest);
        SearchParameterList jurisdictionParam = keyMap.get(JURISDICTION);
        SearchParameterList stateParam = keyMap.get(STATE);
        SearchParameterList locationParam = keyMap.get(LOCATION);
        SearchParameterList caseIdParam = keyMap.get(CASE_ID);
        SearchParameterList userParam = keyMap.get(USER);
        SearchParameterList workTypeParam = keyMap.get(WORK_TYPE);
        SearchParameterList roleCtgParam = keyMap.get(ROLE_CATEGORY);

        final EnumMap<SearchParameterKey, SearchParameterBoolean> boolKeyMap = asEnumMapForBoolean(searchTaskRequest);
        SearchParameterBoolean availableTasksOnly = boolKeyMap.get(AVAILABLE_TASKS_ONLY);

        if (availableTasksOnly != null && availableTasksOnly.getValues()) {
            permissionsRequired.add(PermissionTypes.OWN);
        }
        return searchByJurisdiction(jurisdictionParam == null ? Collections.emptyList() : jurisdictionParam.getValues())
            .and(searchByState(getCftTaskStates(stateParam))
            .and(searchByLocation(locationParam == null ? Collections.emptyList() : locationParam.getValues())
            .and(searchByCaseIds(caseIdParam == null ? Collections.emptyList() : caseIdParam.getValues())
            .and(searchByUser(userParam == null ? Collections.emptyList() : userParam.getValues())
            .and(searchByWorkType(workTypeParam == null ? Collections.emptyList() : workTypeParam.getValues())
            .and(searchByRoleCategory(roleCtgParam == null ? Collections.emptyList() : roleCtgParam.getValues())))))));
    }

    private static List<CFTTaskState> getCftTaskStates(SearchParameterList stateParam) {
        List<CFTTaskState> cftTaskStates = new ArrayList<>();
        if (stateParam != null) {
            final List<String> values = stateParam.getValues();
            if (!values.isEmpty()) {
                cftTaskStates = values.stream()
                    .filter(StringUtils::hasText)
                    .map(value -> CFTTaskState.valueOf(value.toUpperCase(Locale.ROOT)))
                    .collect(Collectors.toList());
            }
        }
        return cftTaskStates;
    }

    private static EnumMap<SearchParameterKey, SearchParameterList> asEnumMapForListOfStrings(
        SearchTaskRequest searchTaskRequest) {

        EnumMap<SearchParameterKey, SearchParameterList> map = new EnumMap<>(SearchParameterKey.class);
        if (searchTaskRequest != null && searchTaskRequest.getSearchParameters() != null) {
            searchTaskRequest.getSearchParameters()
                .stream()
                .filter(SearchParameterList.class::isInstance)
                .forEach(request -> map.put(request.getKey(), (SearchParameterList) request));
        }

        return map;
    }

    private static EnumMap<SearchParameterKey, SearchParameterBoolean> asEnumMapForBoolean(
        SearchTaskRequest searchTaskRequest) {

        EnumMap<SearchParameterKey, SearchParameterBoolean> map = new EnumMap<>(SearchParameterKey.class);
        if (searchTaskRequest != null && searchTaskRequest.getSearchParameters() != null) {
            searchTaskRequest.getSearchParameters()
                .stream()
                .filter(SearchParameterBoolean.class::isInstance)
                .forEach(request -> map.put(request.getKey(), (SearchParameterBoolean) request));
        }

        return map;
    }

}
