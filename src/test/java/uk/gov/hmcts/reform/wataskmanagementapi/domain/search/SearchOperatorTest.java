package uk.gov.hmcts.reform.wataskmanagementapi.domain.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchOperatorTest {

    @Test
    void testSearchOperatorReturnsCorrectValueForIn() {
        SearchOperator result = SearchOperator.from("IN");
        assertEquals(SearchOperator.IN, result);
    }

    @Test
    void testSearchOperatorReturnsCorrectValueForIs() {
        SearchOperator result = SearchOperator.from("CONTEXT");
        assertEquals(SearchOperator.CONTEXT, result);
    }

    @Test
    void testSearchOperatorReturnsCorrectValueForBetween() {
        SearchOperator result = SearchOperator.from("BETWEEN");
        assertEquals(SearchOperator.BETWEEN, result);
    }

    @Test
    void testSearchOperatorReturnsCorrectValueForBefore() {
        SearchOperator result = SearchOperator.from("BEFORE");
        assertEquals(SearchOperator.BEFORE, result);
    }

    @Test
    void testSearchOperatorReturnsCorrectValueForAfter() {
        SearchOperator result = SearchOperator.from("AFTER");
        assertEquals(SearchOperator.AFTER, result);
    }

    @Test
    void testSearchOperatorSupportsValueThatDoesNoExist() {
        assertThrows(IllegalArgumentException.class, () -> SearchOperator.from("BOOM!"));
    }

}
