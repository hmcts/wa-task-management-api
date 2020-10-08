package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

public class Location {
    private String id;
    private String locationName;

    public Location() {
        //Default constructor for deserialization
        super();
    }

    public Location(String id, String locationName) {
        this.id = id;
        this.locationName = locationName;
    }

    public String getId() {
        return id;
    }

    public String getLocationName() {
        return locationName;
    }
}
