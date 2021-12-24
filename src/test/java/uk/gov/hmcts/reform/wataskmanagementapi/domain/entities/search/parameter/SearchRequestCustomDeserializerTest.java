package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.BadRequestException;

import java.io.IOException;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator.BOOLEAN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator.IN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;

@JsonTest
@ExtendWith(MockitoExtension.class)
class SearchRequestCustomDeserializerTest {

    @Autowired
    private JacksonTester<SearchParameter<?>> json;

    @Mock
    private JsonParser jsonParser;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JsonNode searchNode;
    @Mock
    private JsonNode operatorNode;

    @Mock
    private DeserializationContext deserializationContext;

    @Test
    void should_deserialize_boolean_operator() throws IOException {
        when(operatorNode.asText()).thenReturn("BOOLEAN");
        when(searchNode.get("operator")).thenReturn(operatorNode);
        when(jsonParser.getCodec()).thenReturn(objectMapper);
        when(objectMapper.readTree(jsonParser)).thenReturn(searchNode);
        SearchRequestCustomDeserializer deserializer = new SearchRequestCustomDeserializer();

        deserializer.deserialize(jsonParser, deserializationContext);

        verify(objectMapper, times(1)).treeToValue(searchNode, SearchParameterBoolean.class);
        verify(objectMapper, never()).treeToValue(searchNode, SearchParameterList.class);
    }

    @Test
    void should_deserialize_in_operator() throws IOException {
        when(operatorNode.asText()).thenReturn("IN");
        when(searchNode.get("operator")).thenReturn(operatorNode);
        when(jsonParser.getCodec()).thenReturn(objectMapper);
        when(objectMapper.readTree(jsonParser)).thenReturn(searchNode);
        SearchRequestCustomDeserializer deserializer = new SearchRequestCustomDeserializer();

        deserializer.deserialize(jsonParser, deserializationContext);

        verify(objectMapper, times(1)).treeToValue(searchNode, SearchParameterList.class);
        verify(objectMapper, never()).treeToValue(searchNode, SearchParameterBoolean.class);
    }

    @Test
    void should_throw_exception_when_operator_is_null() throws IOException {
        when(searchNode.get("operator")).thenReturn(null);
        when(jsonParser.getCodec()).thenReturn(objectMapper);
        when(objectMapper.readTree(jsonParser)).thenReturn(searchNode);

        SearchRequestCustomDeserializer deserializer = new SearchRequestCustomDeserializer();
        assertThrows(BadRequestException.class, () -> deserializer.deserialize(jsonParser, deserializationContext));
    }

    @Test
    void should_throw_exception_when_operator_is_invalid() throws IOException {
        lenient().when(operatorNode.asText()).thenReturn("NotValid");
        when(searchNode.get("operator")).thenReturn(operatorNode);
        when(jsonParser.getCodec()).thenReturn(objectMapper);
        when(objectMapper.readTree(jsonParser)).thenReturn(searchNode);

        SearchRequestCustomDeserializer deserializer = new SearchRequestCustomDeserializer();
        assertThrows(BadRequestException.class, () -> deserializer.deserialize(jsonParser, deserializationContext));
    }

    @Test
    void should_deserialize_with_snake_case_available_task_filter_with_boolean_operator() throws IOException {
        String jsonContent = searchParameterJson_withSearchParameterBooleanOnly();

        SearchParameterBoolean searchParameter = (SearchParameterBoolean) this.json.parse(jsonContent).getObject();

        assertEquals(AVAILABLE_TASKS_ONLY, searchParameter.getKey());
        assertEquals(BOOLEAN, searchParameter.getOperator());
        assertTrue(searchParameter.getValues());
    }

    @Test
    void should_deserialize_jurisdiction_filter_with_list_operator() throws IOException {
        String jsonContent = searchParameterJson_withSearchParameterListOnly();

        SearchParameterList searchParameter = (SearchParameterList) this.json.parse(jsonContent).getObject();

        assertEquals(JURISDICTION, searchParameter.getKey());
        assertEquals(IN, searchParameter.getOperator());
        assertTrue(searchParameter.getValues().containsAll(of("ia", "sscs")));
    }

    @NotNull
    private String searchParameterJson_withSearchParameterListOnly() {
        return "       {\n"
               + "          \"key\": \"jurisdiction\",\n"
               + "          \"values\": [\"ia\", \"sscs\"],\n"
               + "          \"operator\": \"IN\"\n"
               + "        }\n";
    }

    @NotNull
    private String searchParameterJson_withSearchParameterBooleanOnly() {
        return "      {\n"
               + "           \"key\": \"available_tasks_only\",\n"
               + "           \"value\": true,\n"
               + "           \"operator\": \"BOOLEAN\"\n"
               + "        }";
    }

}
