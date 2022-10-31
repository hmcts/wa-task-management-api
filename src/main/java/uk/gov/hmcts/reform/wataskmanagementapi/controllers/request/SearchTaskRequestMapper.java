package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID_CAMEL_CASE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.WORK_TYPE;

public final class SearchTaskRequestMapper {

    public static final List<String> ALLOWED_WORK_TYPES = List.of(
        "hearing_work", "upper_tribunal", "routine_work", "decision_making_work",
        "applications", "priority", "access_requests", "error_management",
        "review_case", "evidence", "follow_up"
    );

    public static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchTaskRequest map(SearchTaskRequest
                                     clientRequest) {
        uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchTaskRequest request =
            new uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchTaskRequest();
        final EnumMap<SearchParameterKey, SearchParameterList> keyMap = asEnumMapForListOfStrings(clientRequest);

        boolean availableTasksOnly = isAvailableTasksOnly(clientRequest);

        List<CFTTaskState> cftTaskStates = new ArrayList<>();
        if (availableTasksOnly) {
            cftTaskStates.add(CFTTaskState.UNASSIGNED);
        } else {
            SearchParameterList stateParam = keyMap.get(STATE);
            cftTaskStates = getCftTaskStates(stateParam);
        }
        final SearchParameterList jurisdictionParam = keyMap.get(JURISDICTION);
        final SearchParameterList locationParam = keyMap.get(LOCATION);
        SearchParameterList caseIdParam = keyMap.get(CASE_ID);
        if (caseIdParam == null) {
            caseIdParam = keyMap.get(CASE_ID_CAMEL_CASE);
        }
        final SearchParameterList userParam = keyMap.get(USER);
        final SearchParameterList workTypeParam = keyMap.get(WORK_TYPE);
        final SearchParameterList roleCtgParam = keyMap.get(ROLE_CATEGORY);
        final SearchParameterList taskTypeParam = keyMap.get(TASK_TYPE);

        request.setCftTaskStates(cftTaskStates);
        request.setJurisdictions(jurisdictionParam == null ? List.of() : jurisdictionParam.getValues());
        request.setLocations( locationParam == null ? Collections.emptyList() : locationParam.getValues());
        request.setCaseIds(caseIdParam == null ? Collections.emptyList() : caseIdParam.getValues());
        request.setUsers(userParam == null ? Collections.emptyList() : userParam.getValues());
        request.setWorkTypes(workTypeParam == null ? Collections.emptyList() : workTypeParam.getValues());
        request.setRoleCategories(roleCtgParam == null ? Collections.emptyList() : roleCtgParam.getValues());
        request.setTaskTypes(taskTypeParam == null ? Collections.emptyList() : taskTypeParam.getValues());

        return request;
    }

    private void validateRequest(uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchTaskRequest searchTaskRequest, boolean isGranularPermissionEnabled) {
        List<Violation> violations = new ArrayList<>();

            List<String> values = searchTaskRequest.getWorkTypes();
            //Validate
            values.forEach(value -> {
                if (!ALLOWED_WORK_TYPES.contains(value)) {
                    violations.add(new Violation(
                        value,
                        WORK_TYPE.value() + " must be one of " + Arrays.toString(ALLOWED_WORK_TYPES.toArray())
                    ));
                }
            });

        if (isGranularPermissionEnabled) {
            final EnumMap<SearchParameterKey, SearchParameterBoolean> boolKeyMap =
                asEnumMapForBoolean(searchTaskRequest);
            if (boolKeyMap.containsKey(AVAILABLE_TASKS_ONLY)) {
                violations.add(new Violation(AVAILABLE_TASKS_ONLY.value(), "Invalid request parameter"));
            }
        }

        if (!violations.isEmpty()) {
            throw new CustomConstraintViolationException(violations);
        }
    }

    // TODO: Once the granular permission feature flag enabled or available_tasks_only parameter is depreciated,
    // this method should only check AVAILABLE_TASK_ONLY context
    private static boolean isAvailableTasksOnly(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameterBoolean> boolKeyMap = asEnumMapForBoolean(searchTaskRequest);
        SearchParameterBoolean availableTasksOnly = boolKeyMap.get(AVAILABLE_TASKS_ONLY);

        RequestContext context = searchTaskRequest.getRequestContext();

        if (context == null) {
            return availableTasksOnly != null && availableTasksOnly.getValues();
        } else {
            return context.equals(RequestContext.AVAILABLE_TASKS);
        }
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
}
