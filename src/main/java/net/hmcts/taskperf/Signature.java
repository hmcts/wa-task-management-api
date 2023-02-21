package net.hmcts.taskperf;

import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class Signature {
    private String jurisdiction;
    private String region;
    private String location;
    private String caseId;
    private String permissions;
    private String classification;
    private String roleName;
    private String authorisations;

    public Signature(String signature) {
        List<String> values = Arrays.asList(signature.split(":"));
        jurisdiction = values.get(0);
        region = values.get(1);
        location = values.get(2);
        roleName = values.get(3);
        caseId = values.get(4);
        permissions = values.get(5);
        classification = values.get(6);

        StringBuilder skillStr = new StringBuilder();
        for (int i = 7; i < values.size(); i++) {
            skillStr.append(values.get(i));
            skillStr.append(":");
        }
        authorisations = skillStr.substring(0, skillStr.length() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Signature signature = (Signature) o;
        return jurisdiction.equals(signature.jurisdiction) && region.equals(signature.region) && location.equals(
            signature.location) && caseId.equals(signature.caseId) && permissions.equals(signature.permissions) && classification.equals(
            signature.classification) && roleName.equals(signature.roleName) && authorisations.equals(signature.authorisations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            jurisdiction,
            region,
            location,
            caseId,
            permissions,
            classification,
            roleName,
            authorisations
        );
    }

    @Override
    public String toString() {
        return jurisdiction + ":"
            + region + ":"
            + location + ":"
            + roleName + ":"
            + caseId + ":"
            + permissions + ":"
            + classification + ":"
            + authorisations;
    }

}
