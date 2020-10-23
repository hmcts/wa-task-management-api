package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

public class CaseData {

    private String reference;
    private String name;
    private String category;
    private Location location;

    public CaseData() {
        //Default constructor for deserialization
        super();
    }

    public CaseData(String reference, String name, String category, Location location) {
        this.reference = reference;
        this.name = name;
        this.category = category;
        this.location = location;
    }

    public String getReference() {
        return reference;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Location getLocation() {
        return location;
    }
}
