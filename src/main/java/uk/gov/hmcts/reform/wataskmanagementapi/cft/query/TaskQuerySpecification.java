package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@SuppressWarnings({"PMD.TooManyMethods"})
public final class TaskQuerySpecification {

    private static final String STATE = "state";
    private static final String LOCATION = "location";
    private static final String TASK_ID = "taskId";
    private static final String TASK_TYPE = "taskType";
    private static final String ASSIGNEE = "assignee";
    private static final String CASE_ID = "caseId";
    private static final String JURISDICTION = "jurisdiction";
    private static final String WORK_TYPE = "workTypeResource";
    private static final String WORK_TYPE_ID = "id";


    private TaskQuerySpecification() {
        // avoid creating object
    }

    public static Specification<TaskResource> searchByState(List<CFTTaskState> cftTaskStates) {
        if (!CollectionUtils.isEmpty(cftTaskStates)) {
            return (root, query, builder) -> builder.in(root.get(STATE))
                .value(cftTaskStates);
        }
        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByJurisdiction(List<String> jurisdictions) {
        if (!CollectionUtils.isEmpty(jurisdictions)) {
            return (root, query, builder) -> builder.in(root.get(JURISDICTION))
                .value(jurisdictions);
        }
        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByLocation(List<String> locations) {
        if (!CollectionUtils.isEmpty(locations)) {
            return (root, query, builder) -> builder.in(root.get(LOCATION))
                .value(locations);
        }
        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByCaseId(String caseId) {
        return (root, query, builder) -> builder.equal(root.get(CASE_ID), caseId);
    }

    public static Specification<TaskResource> searchByCaseIds(List<String> caseIds) {
        if (!CollectionUtils.isEmpty(caseIds)) {
            return (root, query, builder) -> builder.in(root.get(CASE_ID))
                .value(caseIds);
        }
        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByUser(List<String> users) {
        if (!CollectionUtils.isEmpty(users)) {
            return (root, query, builder) -> builder.in(root.get(ASSIGNEE))
                .value(users);
        }
        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByTaskId(String taskId) {
        return (root, query, builder) -> builder.equal(root.get(TASK_ID), taskId);
    }

    public static Specification<TaskResource> searchByTaskTypes(List<String> taskTypes) {
        if (!CollectionUtils.isEmpty(taskTypes)) {
            return (root, query, builder) -> builder.in(root.get(TASK_TYPE))
                .value(taskTypes);
        }
        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> searchByWorkType(List<String> workTypes) {
        if (!CollectionUtils.isEmpty(workTypes)) {
            return (root, query, builder) -> builder.in(root.get(WORK_TYPE).get(WORK_TYPE_ID))
                .value(workTypes);
        }
        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> extractState(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.STATE) != null) {
            final List<String> values = keyMap.get(SearchParameterKey.STATE).getValues();
            final List<CFTTaskState> cftTaskStates = values.stream()
                .filter(state -> state.trim().length() != 0)
                .map(value -> CFTTaskState.valueOf(value.toUpperCase(Locale.ROOT))).collect(Collectors.toList());

            return searchByState(cftTaskStates);
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> extractJurisdiction(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.JURISDICTION) != null) {
            return searchByJurisdiction(keyMap.get(SearchParameterKey.JURISDICTION).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> extractLocation(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.LOCATION) != null) {
            return searchByLocation(keyMap.get(SearchParameterKey.LOCATION).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> extractCaseId(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.CASE_ID) != null) {
            return searchByCaseIds(keyMap.get(SearchParameterKey.CASE_ID).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> extractUser(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.USER) != null) {
            return searchByUser(keyMap.get(SearchParameterKey.USER).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static Specification<TaskResource> extractWorkType(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameter> keyMap = asEnumMap(searchTaskRequest);
        if (keyMap.get(SearchParameterKey.WORK_TYPE) != null) {
            return searchByWorkType(keyMap.get(SearchParameterKey.WORK_TYPE).getValues());
        }

        return (root, query, builder) -> builder.conjunction();
    }

    public static EnumMap<SearchParameterKey, SearchParameter> asEnumMap(SearchTaskRequest searchTaskRequest) {
        EnumMap<SearchParameterKey, SearchParameter> map = new EnumMap<>(SearchParameterKey.class);
        if (searchTaskRequest != null && searchTaskRequest.getSearchParameters() != null) {
            searchTaskRequest.getSearchParameters()
                .forEach(request -> map.put(request.getKey(), request));
        }

        return map;
    }

}
