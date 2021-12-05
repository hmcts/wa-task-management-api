package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;

import javax.validation.constraints.NotEmpty;
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
    @NotEmpty(
        message = "Each search_parameter element must have 'key', 'value' and 'operator' fields present and populated."
    )
    private final Boolean value;

    @JsonCreator
    public SearchParameterBoolean(SearchParameterKey key, SearchOperator operator, Boolean value) {
        this.key = key;
        this.operator = operator;
        this.value = value;
    }

    public SearchParameterKey getKey() {
        return key;
    }

    public SearchOperator getOperator() {
        return operator;
    }

    public Boolean getValues() {
        return value;
    }
}
