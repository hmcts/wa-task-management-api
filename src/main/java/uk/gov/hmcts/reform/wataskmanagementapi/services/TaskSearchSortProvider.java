package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField.MAJOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField.MINOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField.PRIORITY_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField.TASK_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder.ASCENDANT;

public final class TaskSearchSortProvider {

    private TaskSearchSortProvider() {
    }

    public static Sort getSortOrders(SearchRequest searchRequest) {
        List<Sort.Order> orders = Stream.ofNullable(searchRequest.getSortingParameters())
            .flatMap(Collection::stream)
            .filter(s -> s.getSortOrder() != null)
            .map(sortingParameter -> {
                if (sortingParameter.getSortOrder() == ASCENDANT) {
                    return Sort.Order.asc(sortingParameter.getSortBy().getCftVariableName());
                } else {
                    return Sort.Order.desc(sortingParameter.getSortBy().getCftVariableName());
                }
            }).collect(Collectors.toList()); //NOSONAR List needs to be mutable to allow sorting.

        Stream.of(MAJOR_PRIORITY, PRIORITY_DATE, MINOR_PRIORITY, TASK_ID)
            .map(x -> Sort.Order.asc(x.getCftVariableName()))
            .collect(Collectors.toCollection(() -> orders));

        return Sort.by(orders);
    }

    public static String getSortOrderQuery(SearchRequest searchRequest) {
        StringBuilder orderColumns = new StringBuilder("ORDER BY ");
        List<SortingParameter> sortingParameters = searchRequest.getSortingParameters();

        if (!CollectionUtils.isEmpty(sortingParameters)) {
            for (SortingParameter sortBy : sortingParameters) {
                orderColumns.append(sortBy.getSortBy().getDbColumnName())
                    .append(' ')
                    .append(sortBy.getSortOrder().toString().toUpperCase(Locale.ROOT))
                    .append(", ");
            }
        }
        orderColumns.append(MAJOR_PRIORITY.getDbColumnName())
            .append(' ')
            .append(ASCENDANT.toString().toUpperCase(Locale.ROOT))
            .append(", ")
            .append(PRIORITY_DATE.getDbColumnName())
            .append(' ')
            .append(ASCENDANT.toString().toUpperCase(Locale.ROOT))
            .append(", ")
            .append(MINOR_PRIORITY.getDbColumnName())
            .append(' ')
            .append(ASCENDANT.toString().toUpperCase(Locale.ROOT))
            .append(", ")
            .append(TASK_ID.getDbColumnName())
            .append(' ')
            .append(ASCENDANT.toString().toUpperCase(Locale.ROOT))
            .append(' ');

        return orderColumns.toString();
    }
}
