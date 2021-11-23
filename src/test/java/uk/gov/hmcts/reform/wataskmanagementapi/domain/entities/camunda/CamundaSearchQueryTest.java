package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.sorting.CamundaSortingExpression;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery.CamundaAndQueryBuilder.camundaQuery;

@SuppressWarnings("checkstyle:LineLength")
class CamundaSearchQueryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_set_default_structure() throws JsonProcessingException {

        CamundaSearchQuery camundaSearchQuery = camundaQuery().build();

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);

        String expected = "{\"queries\":{\"orQueries\":[]}}";

        assertEquals(expected, resultJson);
    }

    @Test
    void should_set_key_value() throws JsonProcessingException {

        CamundaSearchQuery camundaSearchQuery = camundaQuery().withKeyValue("aKey", "aValue").build();

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);

        String expected = "{\"queries\":{\"aKey\":\"aValue\",\"orQueries\":[]}}";

        assertEquals(expected, resultJson);
    }

    @Test
    void should_set_and_query() throws JsonProcessingException {

        CamundaSearchQuery camundaSearchQuery = camundaQuery()
            .andQuery(
                new CamundaSearchExpression("aKey", CamundaOperator.EQUAL.toString(), "aValue")
            )
            .build();

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);

        String expected = "{\"queries\":{\"orQueries\":[{\"taskVariables\":[{\"name\":\"aKey\",\"operator\":\"eq\",\"value\":\"aValue\"}]}]}}";

        assertEquals(expected, resultJson);
    }

    @Test
    void should_set_and_sorting_query() throws JsonProcessingException {

        CamundaSearchQuery camundaSearchQuery = camundaQuery()
            .andSortingQuery(
                singletonList(
                    new CamundaSortingExpression("sortByValue", "sortOrderValue")
                )
            )
            .build();

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);

        String expected = "{\"queries\":{\"orQueries\":[],\"sorting\":[{\"sortBy\":\"sortByValue\",\"sortOrder\":\"sortOrderValue\"}]}}";

        assertEquals(expected, resultJson);
    }

    @ParameterizedTest
    @CsvSource({
        "eq, EQUAL",
        "neq, NOT_EQUAL",
        "gt, GREATER_THAN",
        "gteq, GREATER_THAN_OR_EQUAL",
        "lt, LOWER_THAN",
        "lteq, LOWER_THAN_OR_EQUAL",
        "like, LIKE"
    })
    void should_return_camunda_operator_when_input_is_a_valid_string(String valueInput, String operatorInput) {

        assertEquals(
            CamundaOperator.from(valueInput),
            CamundaOperator.valueOf(operatorInput)
        );

    }

    @ParameterizedTest
    @CsvSource(
        value = {
            ",",       // null
            "''",      // empty
            "' '",     // blank
            "123",
            "null",
            "some-value"
        }
    )
    void should_throw_exception_when_invalid_input_given(String input) {

        assertThatThrownBy(() -> CamundaOperator.from(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(input + " is an unsupported operator");

    }

    @Test
    void should_set_camunda_search_query_using_camunda_or_query_builder() throws JsonProcessingException {

        CamundaSearchQuery camundaSearchQuery = camundaQuery()
            .andQuery(
                new CamundaOrQuery.CamundaOrQueryBuilder().query(
                    new CamundaSearchExpression("aKey", CamundaOperator.EQUAL.toString(), "aValue")
                )
            )
            .build();

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);

        String expected = "{\"queries\":{\"orQueries\":[{\"taskVariables\":[{\"name\":\"aKey\",\"operator\":\"eq\",\"value\":\"aValue\"}]}]}}";

        assertEquals(expected, resultJson);
    }

}
