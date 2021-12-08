package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator.BOOLEAN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator.IN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;

@JsonTest
@ExtendWith(MockitoExtension.class)
class SearchRequestCustomDeserializerTest {

    @Autowired
    private JacksonTester<SearchParameter<?>> json;

    @Test
    void testTheSearchParameterBooleanCanBeDeserialized() throws IOException {
        String jsonContent = searchParameterJson_withSearchParameterBooleanOnly();

        SearchParameterBoolean searchParameter = (SearchParameterBoolean) this.json.parse(jsonContent).getObject();

        assertEquals(AVAILABLE_TASKS_ONLY, searchParameter.getKey());
        assertEquals(BOOLEAN, searchParameter.getOperator());
        assertTrue(searchParameter.getValues());
    }

    @Test
    void testTheSearchParameterListCanBeDeserialized() throws IOException {
        String jsonContent = searchParameterJson_withSearchParameterListOnly();

        SearchParameterList searchParameter = (SearchParameterList) this.json.parse(jsonContent).getObject();

        assertEquals(JURISDICTION, searchParameter.getKey());
        assertEquals(IN, searchParameter.getOperator());
        assertTrue(searchParameter.getValues().containsAll(of("ia", "sscs")));
    }

    @NotNull
    private String searchParameterJson_withSearchParameterListOnly() {
        return
            "        {\n" +
            "            \"key\": \"jurisdiction\",\n" +
            "            \"values\": [\"ia\", \"sscs\"],\n" +
            "            \"operator\": \"IN\"\n" +
            "        }\n";
    }

    @NotNull
    private String searchParameterJson_withSearchParameterBooleanOnly() {
        return
            "        {\n" +
            "            \"key\": \"available_tasks_only\",\n" +
            "            \"value\": true,\n" +
            "            \"operator\": \"BOOLEAN\"\n" +
            "        }";
    }

}
