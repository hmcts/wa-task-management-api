package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.sorting.CamundaSortingExpression;

import static java.util.Collections.singletonList;
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

        String expected = "{\"queries\":{\"orQueries\":[{\"processVariables\":[{\"name\":\"aKey\",\"operator\":\"eq\",\"value\":\"aValue\"}]}]}}";

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
}
