package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class TaskSearchSortProviderTest {

    @Test
    void shouldProvideDefaultSortOrderQuery() {
        String query = TaskSearchSortProvider.getSortOrderQuery(SearchRequest.builder().build());
        assertEquals("ORDER BY major_priority ASC, priority_date ASC, minor_priority ASC ", query);
    }

    @Test
    void shouldProvideSortOrderQueryForCamelCaseRequest() {
        String query = TaskSearchSortProvider.getSortOrderQuery(SearchRequest.builder()
            .sortingParameters(List.of(new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.TASK_TITLE_CAMEL_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.LOCATION_NAME_CAMEL_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.CASE_CATEGORY_CAMEL_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.CASE_ID, SortOrder.ASCENDANT),
                new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.NEXT_HEARING_DATE_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build());
        assertEquals("ORDER BY due_date_time ASC, title ASC, location_name ASC, case_category ASC, "
                     + "case_id ASC, case_name ASC, next_hearing_date ASC, major_priority ASC, priority_date ASC, "
                     + "minor_priority ASC ", query);
    }

    @Test
    void shouldProvideSortOrderQueryForSnakeCaseRequest() {
        String query = TaskSearchSortProvider.getSortOrderQuery(SearchRequest.builder()
            .sortingParameters(List.of(new SortingParameter(SortField.DUE_DATE_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.TASK_TITLE_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.LOCATION_NAME_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.CASE_CATEGORY_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.CASE_NAME_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.NEXT_HEARING_DATE_SNAKE_CASE, SortOrder.DESCENDANT)))
            .build());
        assertEquals("ORDER BY due_date_time DESC, title DESC, location_name DESC, case_category DESC, "
                     + "case_id DESC, case_name DESC, next_hearing_date DESC, major_priority ASC, priority_date ASC, "
                     + "minor_priority ASC ", query);
    }

    @Test
    void shouldProvideDefaultSortOrder() {
        Sort query = TaskSearchSortProvider.getSortOrders(SearchRequest.builder().build());
        assertNotNull(query.getOrderFor(SortField.MAJOR_PRIORITY.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.MINOR_PRIORITY.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.PRIORITY_DATE.getCftVariableName()));
    }

    @Test
    void shouldProvideSortOrderForCamelCaseRequest() {
        Sort query = TaskSearchSortProvider.getSortOrders(SearchRequest.builder()
            .sortingParameters(List.of(new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.TASK_TITLE_CAMEL_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.LOCATION_NAME_CAMEL_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.CASE_CATEGORY_CAMEL_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.CASE_ID, SortOrder.ASCENDANT),
                new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.NEXT_HEARING_DATE_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build());
        assertNotNull(query.getOrderFor(SortField.DUE_DATE_CAMEL_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.TASK_TITLE_CAMEL_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.LOCATION_NAME_CAMEL_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.CASE_CATEGORY_CAMEL_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.CASE_ID.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.CASE_NAME_CAMEL_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.NEXT_HEARING_DATE_CAMEL_CASE.getCftVariableName()));
    }

    @Test
    void shouldProvideSortOrderForSnakeCaseRequest() {
        Sort query = TaskSearchSortProvider.getSortOrders(SearchRequest.builder()
            .sortingParameters(List.of(new SortingParameter(SortField.DUE_DATE_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.TASK_TITLE_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.LOCATION_NAME_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.CASE_CATEGORY_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.CASE_NAME_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.NEXT_HEARING_DATE_SNAKE_CASE, SortOrder.DESCENDANT)))
            .build());
        assertNotNull(query.getOrderFor(SortField.DUE_DATE_SNAKE_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.TASK_TITLE_SNAKE_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.LOCATION_NAME_SNAKE_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.CASE_CATEGORY_SNAKE_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.CASE_ID_SNAKE_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.CASE_NAME_SNAKE_CASE.getCftVariableName()));
        assertNotNull(query.getOrderFor(SortField.NEXT_HEARING_DATE_SNAKE_CASE.getCftVariableName()));
    }
}