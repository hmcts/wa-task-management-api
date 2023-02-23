package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.sorting;

public class CamundaSortingExpression {

    private String sortBy;
    private String sortOrder;


    public CamundaSortingExpression() {
        //No-op constructor for deserialization
        super();
    }

    public CamundaSortingExpression(String sortBy,
                                    String sortOrder) {
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

}
