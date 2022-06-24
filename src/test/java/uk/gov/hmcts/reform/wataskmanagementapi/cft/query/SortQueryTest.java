package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;

@ExtendWith(MockitoExtension.class)
public class SortQueryTest {

    @Test
    public void sort_fields_in_descending_order() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, asList(
                "1623278362431003"
            ))
        ), List.of(new SortingParameter(SortField.LOCATION_NAME_CAMEL_CASE, SortOrder.DESCENDANT)));

        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        assertNotNull(sort);
        assertEquals(Sort.by("locationName").descending(), sort);
    }

    @Test
    public void sort_fields_in_ascending_order() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, asList(
                "1623278362431003"
            ))
        ), List.of(new SortingParameter(SortField.LOCATION_NAME_CAMEL_CASE, SortOrder.ASCENDANT)));

        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        assertNotNull(sort);
        assertEquals(Sort.by("locationName").ascending(), sort);
    }

    @Test
    public void sort_by_fields_when_sort_parameters_empty() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, asList(
                "1623278362431003"
            ))));

        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        assertNotNull(sort);
        assertEquals(Sort.by("majorPriority").ascending()
                         .and(Sort.by("priorityDate").descending())
                         .and(Sort.by("minorPriority").ascending()), sort);
    }

    @ParameterizedTest
    @CsvSource(
        value = {
            "DUE_DATE_CAMEL_CASE, dueDate",
            "DUE_DATE_SNAKE_CASE, due_date",
            "TASK_TITLE_CAMEL_CASE, taskTitle",
            "TASK_TITLE_SNAKE_CASE, task_title",
            "LOCATION_NAME_CAMEL_CASE, locationName",
            "LOCATION_NAME_SNAKE_CASE, location_name",
            "CASE_CATEGORY_CAMEL_CASE, caseCategory",
            "CASE_CATEGORY_SNAKE_CASE, case_category",
            "CASE_ID_CAMEL_CASE, caseId",
            "CASE_ID_SNAKE_CASE, case_id",
            "CASE_NAME_CAMEL_CASE, caseName",
            "CASE_NAME_SNAKE_CASE, case_name"
        }
    )
    public void should_return_id_when_toString_method_called(String input, String expected) {

        assertEquals(expected, SortField.valueOf(input).toString());

    }
}
