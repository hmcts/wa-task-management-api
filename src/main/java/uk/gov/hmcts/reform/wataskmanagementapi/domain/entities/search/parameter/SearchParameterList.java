package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@ApiModel(
    value = "SearchParameter",
    description = "Search parameter containing the key, operator and a list of values"
)
@EqualsAndHashCode
@ToString
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class SearchParameterList implements SearchParameter<List<String>> {

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

    @ApiModelProperty(required = true, example = "[\"998db99b-08aa-43d4-bc6b-0aabbb0e3c6f\"]", allowEmptyValue = false)
    @NotNull(
        message = "Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated."
    )
    @NotEmpty(
        message = "Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated."
    )
    private final List<String> value;

    @JsonCreator
    public SearchParameterList(SearchParameterKey key, SearchOperator operator, List<String> value) {
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

    public List<String> getValues() {
        return value;
    }
}
