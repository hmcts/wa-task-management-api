package uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator;

import javax.validation.constraints.NotNull;

@Schema(
    name = "SearchParameterBoolean",
    description = "Search parameter containing the key, operator and boolean values"
)
@EqualsAndHashCode
@ToString
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class SearchParameterBoolean implements SearchParameter<Boolean> {

    @Schema(required = true)
    @NotNull(
        message = "Each search_parameter element must have 'key', 'value' and 'operator' fields present and populated."
    )
    private final SearchParameterKey key;

    @Schema(allowableValues = "BOOLEAN", example = "BOOLEAN")
    @NotNull(
        message = "Each search_parameter element must have 'key', 'value' and 'operator' fields present and populated."
    )
    private final SearchOperator operator;

    @Schema(required = true, example = "true")
    @NotNull(
        message = "Each search_parameter element must have 'key', 'value' and 'operator' fields present and populated."
    )
    private final boolean values;

    @JsonCreator
    public SearchParameterBoolean(SearchParameterKey key, SearchOperator operator, boolean values) {
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
    public Boolean getValues() {
        return values;
    }
}
