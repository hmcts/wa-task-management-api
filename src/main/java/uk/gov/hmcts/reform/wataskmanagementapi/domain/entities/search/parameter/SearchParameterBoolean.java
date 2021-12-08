package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;

import javax.validation.constraints.NotNull;

@ApiModel(
    value = "SearchParameter",
    description = "Search parameter containing the key, operator and a list of values"
)
@EqualsAndHashCode
@ToString
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class SearchParameterBoolean implements SearchParameter<Boolean> {

    @ApiModelProperty(
        required = true,
        allowableValues = "available_tasks_only",
        example = "available_tasks_only")
    @NotNull(
        message = "Each search_parameter element must have 'key', 'value' and 'operator' fields present and populated."
    )
    private final SearchParameterKey key;

    @ApiModelProperty(allowableValues = "BOOLEAN", example = "BOOLEAN")
    @NotNull(
        message = "Each search_parameter element must have 'key', 'value' and 'operator' fields present and populated."
    )
    private final SearchOperator operator;

    @ApiModelProperty(required = true, example = "true", allowEmptyValue = false)
    @NotNull(
        message = "Each search_parameter element must have 'key', 'value' and 'operator' fields present and populated."
    )
    private final Boolean values;

    @JsonCreator
    public SearchParameterBoolean(SearchParameterKey key, SearchOperator operator, Boolean values) {
        this.key = key;
        this.operator = operator;
        this.values = values;
    }

    public SearchParameterKey getKey() {
        return key;
    }

    public SearchOperator getOperator() {
        return operator;
    }

    @JsonProperty("value")
    public Boolean getValues() {
        return values;
    }
}
