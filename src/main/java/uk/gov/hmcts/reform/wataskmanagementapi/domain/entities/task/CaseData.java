package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import java.util.Objects;

public class CaseData {

    private String reference;
    private String jurisdiction;
    private String name;
    private String category;
    private Location location;

    private CaseData() {
        //Default constructor for deserialization
        super();
    }

    public CaseData(String reference, String jurisdiction, String name, String category, Location location) {
        this.reference = reference;
        this.jurisdiction = jurisdiction;
        this.name = name;
        this.category = category;
        this.location = location;
    }

    public String getReference() {
        return reference;
    }

    public String getJurisdiction() {
        return jurisdiction;
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        CaseData caseData = (CaseData) object;
        return Objects.equals(reference, caseData.reference)
               && Objects.equals(jurisdiction, caseData.jurisdiction)
               && Objects.equals(name, caseData.name)
               && Objects.equals(category, caseData.category)
               && Objects.equals(location, caseData.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, jurisdiction, name, category, location);
    }

    @Override
    public String toString() {
        return "CaseData{"
               + "reference='" + reference + '\''
               + ", jurisdiction='" + jurisdiction + '\''
               + ", name='" + name + '\''
               + ", category='" + category + '\''
               + ", location=" + location
               + '}';
    }
}
