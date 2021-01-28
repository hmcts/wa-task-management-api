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

    @ApiModelProperty(required = true)
    private SearchParameterKey key;
    @ApiModelProperty(required = true)
    private SearchOperator operator;
    @ApiModelProperty(required = true)
    private List<String> values;

    private SearchParameter() {
        //Default constructor for deserialization
        super();
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
