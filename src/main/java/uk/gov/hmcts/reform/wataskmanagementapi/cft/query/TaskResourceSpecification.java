package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
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

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildQueryToRetrieveRoleInformation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildRoleAssignmentConstraints;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByJurisdiction;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByLocation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByRoleCategory;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByState;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskIds;
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

@Slf4j
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

        boolean andPermissions = isAvailableTasksOnly(searchTaskRequest);
        final boolean availableTasksOnly = isAvailableTasksOnly(searchTaskRequest);

        if (andPermissions) {
            permissionsRequired.add(PermissionTypes.OWN);
        }

        log.debug("Querying with 'available_tasks_only' set to '{}'", availableTasksOnly);
        log.debug("Querying with 'permissions required' set to '{}'", permissionsRequired);

        final Specification<TaskResource> constrainsSpec =
            buildApplicationConstraints(searchTaskRequest, availableTasksOnly);

        final Specification<TaskResource> roleAssignmentSpec = buildRoleAssignmentConstraints(
            permissionsRequired, accessControlResponse, andPermissions);

        return constrainsSpec.and(roleAssignmentSpec);

    }

    public static Specification<TaskResource> buildSingleTaskQuery(String taskId,
                                                                   AccessControlResponse accessControlResponse,
                                                                   List<PermissionTypes> permissionsRequired
    ) {
        return searchByTaskIds(singletonList(taskId))
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse, false));
    }

    public static Specification<TaskResource> buildTaskRolePermissionsQuery(
        String taskId,
        AccessControlResponse accessControlResponse) {

        return searchByTaskIds(singletonList(taskId))
            .and(buildQueryToRetrieveRoleInformation(accessControlResponse));
    }

    public static Specification<TaskResource> buildQueryForCompletable(
        SearchEventAndCase searchEventAndCase, AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired, List<String> taskTypes) {

        return searchByCaseId(searchEventAndCase.getCaseId())
            .and(searchByState(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .and(searchByTaskTypes(taskTypes))
            //.and(searchByUser(List.of(accessControlResponse.getUserInfo().getUid())))
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse, false));
    }

    private static Specification<TaskResource> buildApplicationConstraints(SearchTaskRequest searchTaskRequest,
                                                                           boolean availableTasksOnly) {

        final EnumMap<SearchParameterKey, SearchParameterList> keyMap = asEnumMapForListOfStrings(searchTaskRequest);

        List<CFTTaskState> cftTaskStates = new ArrayList<>();
        if (availableTasksOnly) {
            cftTaskStates.add(CFTTaskState.ASSIGNED);
            cftTaskStates.add(CFTTaskState.UNASSIGNED);
        } else {
            SearchParameterList stateParam = keyMap.get(STATE);
            cftTaskStates = getCftTaskStates(stateParam);
        }
        SearchParameterList jurisdictionParam = keyMap.get(JURISDICTION);
        SearchParameterList locationParam = keyMap.get(LOCATION);
        SearchParameterList caseIdParam = keyMap.get(CASE_ID);
        SearchParameterList userParam = keyMap.get(USER);
        SearchParameterList workTypeParam = keyMap.get(WORK_TYPE);
        SearchParameterList roleCtgParam = keyMap.get(ROLE_CATEGORY);

        return searchByJurisdiction(jurisdictionParam == null ? Collections.emptyList() : jurisdictionParam.getValues())
            .and(searchByState(cftTaskStates)
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
        if (searchTaskRequest != null && !CollectionUtils.isEmpty(searchTaskRequest.getSearchParameters())) {
            searchTaskRequest.getSearchParameters()
                .stream()
                .filter(SearchParameterBoolean.class::isInstance)
                .forEach(request -> map.put(request.getKey(), (SearchParameterBoolean) request));
        }

        return map;
    }

    private static boolean isAvailableTasksOnly(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameterBoolean> boolKeyMap = asEnumMapForBoolean(searchTaskRequest);
        SearchParameterBoolean availableTasksOnly = boolKeyMap.get(AVAILABLE_TASKS_ONLY);

        return availableTasksOnly != null && availableTasksOnly.getValues();
    }

}
