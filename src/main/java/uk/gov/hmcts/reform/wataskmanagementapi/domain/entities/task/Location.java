package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import com.google.common.base.Objects;

public class Location {
    private String id;
    private String locationName;

    private Location() {
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Location location = (Location) object;
        return Objects.equal(id, location.id)
               && Objects.equal(locationName, location.locationName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, locationName);
    }

    @Override
    public String toString() {
        return "Location{"
               + "id='" + id + '\''
               + ", locationName='" + locationName + '\''
               + '}';
    }
}
