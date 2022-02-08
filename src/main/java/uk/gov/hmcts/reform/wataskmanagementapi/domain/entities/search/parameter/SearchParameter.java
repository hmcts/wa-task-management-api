package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;

public interface SearchParameter<T> {

    SearchParameterKey getKey();

    SearchOperator getOperator();

    T getValues();
}
