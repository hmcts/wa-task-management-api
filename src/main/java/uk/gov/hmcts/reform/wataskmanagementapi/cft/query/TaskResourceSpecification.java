package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildRoleAssignmentConstraints;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByJurisdiction;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByLocation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByState;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskTypes;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByUser;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByWorkType;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.WORK_TYPE;


@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.TooManyMethods", "PMD.LawOfDemeter"})
public final class TaskResourceSpecification {

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

        return searchByCaseId(searchEventAndCase.getCaseId())
            .and(searchByState(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .and(searchByTaskTypes(taskTypes))
            //.and(searchByUser(List.of(accessControlResponse.getUserInfo().getUid())))
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse));
    }

    private static Specification<TaskResource> buildApplicationConstraints(SearchTaskRequest searchTaskRequest) {

        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        SearchParameter jurisdictionParam = keyMap.get(JURISDICTION);
        SearchParameter stateParam = keyMap.get(STATE);
        SearchParameter locationParam = keyMap.get(LOCATION);
        SearchParameter caseIdParam = keyMap.get(CASE_ID);
        SearchParameter userParam = keyMap.get(USER);
        SearchParameter workTypeParam = keyMap.get(WORK_TYPE);

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

        return searchByJurisdiction(jurisdictionParam == null ? Collections.emptyList() : jurisdictionParam.getValues())
            .and(searchByState(cftTaskStates)
            .and(searchByLocation(locationParam == null ? Collections.emptyList() : locationParam.getValues())
            .and(searchByCaseIds(caseIdParam == null ? Collections.emptyList() : caseIdParam.getValues())
            .and(searchByUser(userParam == null ? Collections.emptyList() : userParam.getValues())
            .and(searchByWorkType(workTypeParam == null ? Collections.emptyList() : workTypeParam.getValues()))))));
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
