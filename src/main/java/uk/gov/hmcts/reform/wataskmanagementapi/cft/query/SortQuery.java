package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

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

    public static List<Order> sortByFields(SearchTaskRequest searchTaskRequest,
                                           CriteriaBuilder builder,
                                           Root<TaskResource> root) {
        final List<SortingParameter> sortingParameters = searchTaskRequest.getSortingParameters();

        List<Order> orders = new ArrayList<>();
        if (sortingParameters == null || sortingParameters.isEmpty()) {
            orders.add(builder.desc(root.get("dueDateTime")));
        } else {
            orders.addAll(generateOrders(sortingParameters, builder, root));
        }

        return orders;
    }

    private static List<Sort> generateSortList(List<SortingParameter> sortingParameters) {
        return sortingParameters.stream().map((sortingParameter) -> {
            switch (sortingParameter.getSortOrder()) {
                case ASCENDANT:
                    return Sort.by(Sort.Order.asc(sortingParameter.getSortBy().getCftVariableName()));
                case DESCENDANT:
                    return Sort.by(Sort.Order.desc(sortingParameter.getSortBy().getCftVariableName()));
                default:
                    return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static List<Order> generateOrders(List<SortingParameter> sortingParameters,
                                              CriteriaBuilder builder,
                                              Root<TaskResource> root) {
        return sortingParameters.stream().map((sortingParameter) -> {
            switch (sortingParameter.getSortOrder()) {
                case ASCENDANT:
                    return builder.asc(root.get(sortingParameter.getSortBy().getCftVariableName()));
                case DESCENDANT:
                    return builder.desc(root.get(sortingParameter.getSortBy().getCftVariableName()));
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
