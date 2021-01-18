package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.sorting;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;

public class CamundaProcessVariableSortingExpression extends CamundaSortingExpression {

    private CamundaSortingParameters parameters;

    public CamundaProcessVariableSortingExpression() {
        //No-op constructor for deserialization
        super();
    }

    public CamundaProcessVariableSortingExpression(String sortBy, String sortOrder, CamundaSortingParameters parameters) {
        super(sortBy, sortOrder);
        this.parameters = parameters;
    }

    public CamundaSortingParameters getParameters() {
        return parameters;
    }
}
