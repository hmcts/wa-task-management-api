package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import java.util.Objects;

public class Assignee {
    private String id;
    private String userName;

    private Assignee() {
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Assignee assignee = (Assignee) object;
        return Objects.equals(id, assignee.id)
               && Objects.equals(userName, assignee.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userName);
    }

    @Override
    public String toString() {
        return "Assignee{"
               + "id='" + id + '\''
               + ", userName='" + userName + '\''
               + '}';
    }
}
