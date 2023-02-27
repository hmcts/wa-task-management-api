package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.sorting;

public class CamundaSortingParameters {

    private String variable;
    private String type;

    public CamundaSortingParameters() {
        //No-op constructor for deserialization
        super();
    }

    public CamundaSortingParameters(String variable, String type) {
        this.variable = variable;
        this.type = type;
    }

    public String getVariable() {
        return variable;
    }

    public String getType() {
        return type;
    }
}
