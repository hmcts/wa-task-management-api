package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

public class ProblemResponse {

    private String type;
    private final String title;
    private final String status;
    private final String detail;

    public ProblemResponse(
        String type,
        String title,
        String status,
        String detail
    ) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

}
