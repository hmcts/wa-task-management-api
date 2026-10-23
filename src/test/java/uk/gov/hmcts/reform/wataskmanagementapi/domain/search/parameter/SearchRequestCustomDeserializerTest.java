package uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.BadRequestException;

import java.io.IOException;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@JsonTest
@ExtendWith(MockitoExtension.class)
class SearchRequestCustomDeserializerTest {

    @Autowired
    private JacksonTester<SearchParameter<?>> json;

    @Mock
    private JsonParser jsonParser;

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
        when(deserializationContext.readTree(jsonParser)).thenReturn(searchNode);
        SearchRequestCustomDeserializer deserializer = new SearchRequestCustomDeserializer();

        deserializer.deserialize(jsonParser, deserializationContext);

        verify(deserializationContext, times(1)).readTreeAsValue(searchNode, SearchParameterBoolean.class);
        verify(deserializationContext, never()).readTreeAsValue(searchNode, SearchParameterList.class);
    }

    @Test
    void should_use_explicit_handled_type_when_provided() {
        SearchRequestCustomDeserializer deserializer = new SearchRequestCustomDeserializer(SearchParameterList.class);

        Assertions.assertEquals(SearchParameterList.class, deserializer.handledType());
    }

    @Test
    void should_deserialize_in_operator() throws IOException {
        when(operatorNode.asText()).thenReturn("IN");
        when(searchNode.get("operator")).thenReturn(operatorNode);
        when(deserializationContext.readTree(jsonParser)).thenReturn(searchNode);
        SearchRequestCustomDeserializer deserializer = new SearchRequestCustomDeserializer();

        deserializer.deserialize(jsonParser, deserializationContext);

        verify(deserializationContext, times(1)).readTreeAsValue(searchNode, SearchParameterList.class);
        verify(deserializationContext, never()).readTreeAsValue(searchNode, SearchParameterBoolean.class);
    }

    @Test
    void should_throw_exception_when_operator_is_null() throws IOException {
        when(searchNode.get("operator")).thenReturn(null);
        when(deserializationContext.readTree(jsonParser)).thenReturn(searchNode);

        SearchRequestCustomDeserializer deserializer = new SearchRequestCustomDeserializer();
        assertThrows(BadRequestException.class, () -> deserializer.deserialize(jsonParser, deserializationContext));
    }

    @Test
    void should_throw_exception_when_operator_is_invalid() throws IOException {
        lenient().when(operatorNode.asText()).thenReturn("NotValid");
        when(searchNode.get("operator")).thenReturn(operatorNode);
        when(deserializationContext.readTree(jsonParser)).thenReturn(searchNode);

        SearchRequestCustomDeserializer deserializer = new SearchRequestCustomDeserializer();
        assertThrows(BadRequestException.class, () -> deserializer.deserialize(jsonParser, deserializationContext));
    }


    @Test
    void should_deserialize_jurisdiction_filter_with_list_operator() throws IOException {
        String jsonContent = searchParameterJson_withSearchParameterListOnly();

        SearchParameterList searchParameter = (SearchParameterList) this.json.parse(jsonContent).getObject();

        Assertions.assertEquals(SearchParameterKey.JURISDICTION, searchParameter.getKey());
        Assertions.assertEquals(SearchOperator.IN, searchParameter.getOperator());
        assertTrue(searchParameter.getValues().containsAll(of("ia", "sscs")));
    }

    @NotNull
    private String searchParameterJson_withSearchParameterListOnly() {
        return """
            {
              "key": "jurisdiction",
              "operator": "IN",
              "values": ["ia", "sscs"]         
            }
            """;
    }

}
