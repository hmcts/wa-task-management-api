package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SortQuery {

    private SortQuery() {
        // avoid creating object
    }

    public static Sort sortByFields(SearchTaskRequest searchTaskRequest) {
        final List<SortingParameter> sortingParameters = searchTaskRequest.getSortingParameters();

        Sort sort;
        if (sortingParameters == null || sortingParameters.isEmpty()) {
            sort = Sort.by("dueDateTime").descending();
        } else {
            final List<Sort> sortList = generateSortList(sortingParameters);
            sort = andSort(sortList).orElse(Sort.unsorted());
        }

        return sort;
    }

    private static List<Sort> generateSortList(List<SortingParameter> sortingParameters) {
        return sortingParameters.stream().map((sortingParameter) -> {
            switch (sortingParameter.getSortOrder()) {
                case ASCENDANT:
                    return Sort.by(Sort.Order.asc(sortingParameter.getSortBy().getCamundaVariableName()));
                case DESCENDANT:
                    return Sort.by(Sort.Order.desc(sortingParameter.getSortBy().getCamundaVariableName()));
                default:
                    return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static <V extends Sort> Optional<Sort> andSort(List<V> criteria) {

        Iterator<V> itr = criteria.iterator();
        if (itr.hasNext()) {
            Sort sort = itr.next();
            while (itr.hasNext()) {
                sort = sort.and(itr.next());
            }
            return Optional.of(sort);
        }
        return Optional.empty();
    }
}
