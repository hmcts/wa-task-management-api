package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

public class CamundaSearchExpression {

    private final String name;
    private final String operator;
    private final String value;

    public CamundaSearchExpression(String name, String operator, String value) {
        this.name = name;
        this.operator = operator;
        this.value = value;
    }

    public String getname() {
        return name;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }
}

