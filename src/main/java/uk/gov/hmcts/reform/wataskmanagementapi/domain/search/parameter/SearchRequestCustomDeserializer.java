package uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter;


import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.BadRequestException;

@JacksonComponent
@SuppressWarnings({"PMD.LawOfDemeter"})
public class SearchRequestCustomDeserializer extends StdDeserializer<SearchParameter<?>> {

    private static final long serialVersionUID = -1895766495984179418L;

    private static final String ERROR_MESSAGE =
        "Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated.";

    public SearchRequestCustomDeserializer() {
        this(null);
    }

    public SearchRequestCustomDeserializer(final Class<?> cls) {
        // Jackson 2 tolerated a null handled type; Jackson 3 rejects it.
        // Keep the no-argument Spring/Jackson component constructor valid while
        // still retaining the explicit constructor for callers that provide a type.
        super(cls == null ? SearchParameter.class : cls);
    }

    @Override
    public SearchParameter<?> deserialize(JsonParser jsonParser, DeserializationContext ctxt) {

        final JsonNode searchNode = ctxt.readTree(jsonParser);

        final JsonNode operatorNode = searchNode.get("operator");

        if (operatorNode == null) {
            throw new BadRequestException(ERROR_MESSAGE);
        }

        if (SearchOperator.BOOLEAN.getValue().equals(operatorNode.asText())) {
            return ctxt.readTreeAsValue(searchNode, SearchParameterBoolean.class);
        } else if (SearchOperator.IN.getValue().equals(operatorNode.asText())) {
            return ctxt.readTreeAsValue(searchNode, SearchParameterList.class);
        } else {
            throw new BadRequestException(ERROR_MESSAGE);
        }
    }
}
