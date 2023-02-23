package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

public class CamundaExceptionMessage {

    private String type;
    private String message;

    public CamundaExceptionMessage() {
        super();
        //No-op constructor for deserialization
    }

    public CamundaExceptionMessage(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
