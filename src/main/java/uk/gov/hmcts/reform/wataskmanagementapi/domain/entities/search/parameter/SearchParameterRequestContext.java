package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;

import javax.validation.constraints.NotNull;

@Schema(
    name = "SearchParameterRequestContext",
    description = "Search parameter containing the key, operator and request context"
)
@EqualsAndHashCode
@ToString
public class SearchParameterRequestContext implements SearchParameter<RequestContext> {

    @Schema(
        required = true,
        allowableValues = "request_context",
        example = "request_context")
    @NotNull(
        message = "Each search_parameter element must have 'key', 'value' and 'operator' fields present and populated."
    )
    private final SearchParameterKey key;

    @Schema(allowableValues = "CONTEXT", example = "CONTEXT")
    private final SearchOperator operator;

    @Schema(
        required = true,
        example = "ALL_WORK")
    @NotNull(
        message = "Each search_parameter element must have 'key', 'value' and 'operator' fields present and populated."
    )
    private final RequestContext values;

    @JsonCreator
    public SearchParameterRequestContext(@JsonProperty("key") SearchParameterKey key, @JsonProperty("operator") SearchOperator operator, @JsonProperty("value") RequestContext values) {
        this.key = key;
        this.operator = operator;
        this.values = values;
    }

    @Override
    public SearchParameterKey getKey() {
        return key;
    }

    @Override
    public SearchOperator getOperator() {
        return operator;
    }

    @Override
    @JsonProperty("value")
    public RequestContext getValues() {
        return values;
    }

    public boolean isEqual(RequestContext context) {
        return values != null && values.equals(context);
    }
}
