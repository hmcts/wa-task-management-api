package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;

@ExtendWith(MockitoExtension.class)
public class SortQueryTest {

    @Test
    public void sortFieldsInDescendingOrder() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameter(CASE_ID, SearchOperator.IN, asList(
                "1623278362431003"
            ))
        ), List.of(new SortingParameter(SortField.LOCATION_NAME_CAMEL_CASE, SortOrder.DESCENDANT)));

        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        assertNotNull(sort);
        assertEquals(Sort.by("locationName").descending(), sort);
    }

    @Test
    public void sortFieldsInAscendingOrder() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameter(CASE_ID, SearchOperator.IN, asList(
                "1623278362431003"
            ))
        ), List.of(new SortingParameter(SortField.LOCATION_NAME_CAMEL_CASE, SortOrder.ASCENDANT)));

        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        assertNotNull(sort);
        assertEquals(Sort.by("locationName").ascending(), sort);
    }

    @Test
    public void sortByFieldsWhenSortParametersEmpty() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameter(CASE_ID, SearchOperator.IN, asList(
                "1623278362431003"
            ))));

        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        assertNotNull(sort);
        assertEquals(Sort.by("dueDateTime").descending(), sort);
    }

    @Test
    public void should_return_id_when_sort_toString_method_called() {
        String expectedValue = "dueDate";
        assertEquals(expectedValue, SortField.DUE_DATE_CAMEL_CASE.toString());
    }
}
