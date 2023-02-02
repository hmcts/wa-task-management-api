package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.CASE_ID_CAMEL_CASE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.WORK_TYPE;

public final class SearchTaskRequestMapper {

    private SearchTaskRequestMapper() {
        //Utility class constructor
    }

    public static final List<String> ALLOWED_WORK_TYPES = List.of(
            "hearing_work", "upper_tribunal", "routine_work", "decision_making_work",
            "applications", "priority", "access_requests", "error_management",
            "review_case", "evidence", "follow_up"
    );

    public static SearchRequest map(SearchTaskRequest clientRequest, boolean isGranularPermissionEnabled) {
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
        List<String> workTypes = workTypeParam == null ? Collections.emptyList() : workTypeParam.getValues();
        final SearchParameterList roleCtgParam = keyMap.get(ROLE_CATEGORY);
        final SearchParameterList taskTypeParam = keyMap.get(TASK_TYPE);

        validateRequest(clientRequest, workTypes, isGranularPermissionEnabled);

        return SearchRequest.builder()
            .requestContext(clientRequest.getRequestContext())
            .availableTasksOnly(availableTasksOnly)
            .cftTaskStates(cftTaskStates)
            .jurisdictions(jurisdictionParam == null ? List.of() : jurisdictionParam.getValues())
            .locations(locationParam == null ? Collections.emptyList() : locationParam.getValues())
            .caseIds(caseIdParam == null ? Collections.emptyList() : caseIdParam.getValues())
            .users(userParam == null ? Collections.emptyList() : userParam.getValues())
            .workTypes(workTypes)
            .roleCategories(roleCtgParam == null ? Collections.emptyList() : roleCtgParam.getValues())
            .taskTypes(taskTypeParam == null ? Collections.emptyList() : taskTypeParam.getValues())
            .sortingParameters(clientRequest.getSortingParameters())
            .build();
    }

    private static void validateRequest(SearchTaskRequest searchTaskRequest, List<String> workTypes,
                                 boolean isGranularPermissionEnabled) {
        List<Violation> violations = new ArrayList<>();
        //Validate
        workTypes.forEach(value -> {
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
