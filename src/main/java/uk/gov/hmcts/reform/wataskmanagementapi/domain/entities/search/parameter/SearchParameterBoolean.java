package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;

import javax.validation.constraints.NotNull;

@Schema(
    name = "SearchParameter",
    description = "Search parameter containing the key, operator and a list of values"
)
@EqualsAndHashCode
@ToString
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class SearchParameterBoolean implements SearchParameter<Boolean> {

    @Schema(
        required = true,
        allowableValues = "available_tasks_only",
        example = "available_tasks_only")
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
    public SearchParameterBoolean(@JsonProperty("key") SearchParameterKey key, @JsonProperty("operator") SearchOperator operator, @JsonProperty("value") boolean values) {
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
