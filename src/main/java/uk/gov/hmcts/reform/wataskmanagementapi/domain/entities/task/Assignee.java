package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

public class Assignee {
    private String id;
    private String userName;

    public Assignee() {
        //Default constructor for deserialization
        super();
    }

    public Assignee(String id, String userName) {
        this.id = id;
        this.userName = userName;
    }

    public String getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }
}
