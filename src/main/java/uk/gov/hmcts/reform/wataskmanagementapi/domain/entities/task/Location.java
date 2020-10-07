package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

public class Location {
    private String id;
    private String location;

    public Location() {
        //Default constructor for deserialization
        super();
    }

    public Location(String id, String location) {
        this.id = id;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }
}
