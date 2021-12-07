package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = SearchParameterList.class, name = "search_parameter_list"),
        @JsonSubTypes.Type(value = SearchParameterBoolean.class, name = "search_parameter_boolean")
    })
public interface SearchParameter<T> {

    SearchParameterKey getKey();

    SearchOperator getOperator();

    T getValues();
}
