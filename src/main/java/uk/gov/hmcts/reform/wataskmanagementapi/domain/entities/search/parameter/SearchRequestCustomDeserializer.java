package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.boot.jackson.JsonComponent;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.BadRequestException;

import java.io.IOException;

@JsonComponent
public class SearchRequestCustomDeserializer extends StdDeserializer<SearchParameter> {

    public SearchRequestCustomDeserializer() {
        this(null);
    }

    public SearchRequestCustomDeserializer(final Class<?> cls) {
        super(cls);
    }

    @Override
    public SearchParameter deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {

        final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        final JsonNode searchNode = mapper.readTree(jsonParser);

        final JsonNode operatorNode = searchNode.get("operator");

        String message = "Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated.";

        if (operatorNode == null) {
            throw new BadRequestException(message);
        }

        if (SearchOperator.BOOLEAN.getValue().equals(operatorNode.asText())) {
            return mapper.treeToValue(searchNode, SearchParameterBoolean.class);
        } else if (SearchOperator.IN.getValue().equals(operatorNode.asText())) {
            return mapper.treeToValue(searchNode, SearchParameterList.class);
        } else {
            throw new BadRequestException(message);
        }
    }
}
