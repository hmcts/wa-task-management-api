package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum BusinessContext {

    CFT_TASK("CFT_TASK");

    private String businessContext;

    BusinessContext(String businessContext) {
        this.businessContext = businessContext;
    }

    public String getBusinessContext() {
        return businessContext;
    }
}
