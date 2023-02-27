package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.sorting;

public class CamundaProcessVariableSortingExpression extends CamundaSortingExpression {

    private CamundaSortingParameters parameters;

    public CamundaProcessVariableSortingExpression() {
        //No-op constructor for deserialization
        super();
    }

    public CamundaProcessVariableSortingExpression(String sortBy,
                                                   String sortOrder,
                                                   CamundaSortingParameters parameters) {
        super(sortBy, sortOrder);
        this.parameters = parameters;
    }

    public CamundaSortingParameters getParameters() {
        return parameters;
    }
}
