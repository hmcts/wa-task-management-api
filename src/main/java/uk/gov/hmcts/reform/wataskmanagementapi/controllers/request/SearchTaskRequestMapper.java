package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import org.springframework.util.StringUtils;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

    public static final List<String> ALLOWED_WORK_TYPES = List.of(
        "hearing_work", "upper_tribunal", "routine_work", "decision_making_work",
        "applications", "priority", "access_requests", "error_management",
        "review_case", "evidence", "follow_up"
    );

    private SearchTaskRequestMapper() {
        //Utility class constructor
    }

    public static SearchRequest map(SearchTaskRequest clientRequest) {
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
        List<String> workTypes = getValueOrEmpty(workTypeParam);
        final SearchParameterList roleCtgParam = keyMap.get(ROLE_CATEGORY);
        final SearchParameterList taskTypeParam = keyMap.get(TASK_TYPE);
        final List<SortingParameter> sortingParameters = clientRequest.getSortingParameters();

        validateRequest(workTypes);

        return SearchRequest.builder()
            .requestContext(clientRequest.getRequestContext())
            .availableTasksOnly(availableTasksOnly)
            .cftTaskStates(cftTaskStates)
            .jurisdictions(getValueOrEmpty(jurisdictionParam))
            .locations(getValueOrEmpty(locationParam))
            .caseIds(getValueOrEmpty(caseIdParam))
            .users(getValueOrEmpty(userParam))
            .workTypes(workTypes)
            .roleCategories(getRoleCategory(roleCtgParam))
            .taskTypes(getValueOrEmpty(taskTypeParam))
            .sortingParameters(sortingParameters == null ? List.of() : sortingParameters)
            .build();
    }

    private static void validateRequest(List<String> workTypes) {
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

        if (!violations.isEmpty()) {
            throw new CustomConstraintViolationException(violations);
        }
    }

    private static boolean isAvailableTasksOnly(SearchTaskRequest searchTaskRequest) {

        RequestContext context = searchTaskRequest.getRequestContext();

        return context != null && context.equals(RequestContext.AVAILABLE_TASKS);
    }

    private static List<CFTTaskState> getCftTaskStates(SearchParameterList stateParam) {
        return getValueOrEmpty(stateParam).stream()
            .filter(StringUtils::hasText)
            .map(value -> CFTTaskState.valueOf(value.toUpperCase(Locale.ROOT)))
            .collect(Collectors.toList());
    }

    private static List<RoleCategory> getRoleCategory(SearchParameterList roleCtgParam) {
        return getValueOrEmpty(roleCtgParam).stream()
            .filter(StringUtils::hasText)
            .map(value -> RoleCategory.valueOf(value.toUpperCase(Locale.ROOT)))
            .collect(Collectors.toList());
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

    private static List<String> getValueOrEmpty(SearchParameterList parameterList) {
        return parameterList == null ? List.of() : parameterList.getValues();
    }
}
