package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ApiModel(
    value = "SearchParameter",
    description = "Search parameter containing the key, operator and a list of values"
)
@EqualsAndHashCode
@ToString
public class SearchParameter {

    @ApiModelProperty(
        required = true,
        allowableValues = "location, user, jurisdiction, state, work_type",
        example = "user"
    )
    private SearchParameterKey key;
    @ApiModelProperty(allowableValues = "IN", example = "IN")
    private SearchOperator operator;
    @ApiModelProperty(required = true, example = "998db99b-08aa-43d4-bc6b-0aabbb0e3c6f", allowEmptyValue = true)
    private List<String> values;

    private SearchParameter() {
        //Default constructor for deserialization
    }

    public SearchParameter(SearchParameterKey key, SearchOperator operator, List<String> values) {
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

    public List<String> getValues() {
        return values;
    }
}
