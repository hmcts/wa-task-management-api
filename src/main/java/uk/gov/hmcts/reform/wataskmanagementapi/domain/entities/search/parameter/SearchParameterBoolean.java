package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

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
        allowableValues = "location, user, jurisdiction, state, taskId, taskType, caseId, work_type",
        example = "user")
    @NotNull(
        message = "Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated."
    )
    private final SearchParameterKey key;

    @ApiModelProperty(allowableValues = "IN", example = "IN")
    @NotNull(
        message = "Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated."
    )
    private final SearchOperator operator;

    @ApiModelProperty(required = true, example = "true", allowEmptyValue = false)
    @NotNull(
        message = "Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated."
    )
    @NotEmpty(
        message = "Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated."
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

    public Boolean getValue() {
        return value;
    }
}
