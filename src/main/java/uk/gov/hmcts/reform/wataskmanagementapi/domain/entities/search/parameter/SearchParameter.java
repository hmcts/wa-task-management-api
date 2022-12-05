package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;

@Schema(
    description = "SearchParameters",
    discriminatorProperty = "values",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "SearchParameterBoolean", schema = SearchParameterBoolean.class),
        @DiscriminatorMapping(value = "SearchParameterRequestContext", schema = SearchParameterRequestContext.class),
        @DiscriminatorMapping(value = "SearchParameterList", schema = SearchParameterList.class)
    }
)
public interface SearchParameter<T> {

    SearchParameterKey getKey();

    SearchOperator getOperator();

    T getValues();
}
