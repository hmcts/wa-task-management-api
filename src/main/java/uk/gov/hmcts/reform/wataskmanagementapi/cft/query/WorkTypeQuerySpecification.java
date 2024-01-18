package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;

import java.util.Set;

public final class WorkTypeQuerySpecification {

    public static final String WORK_TYPE_ID = "id";
    public static final int SINGLE_ELEMENT = 1;

    private WorkTypeQuerySpecification() {
        // avoid creating object
    }

    public static Specification<WorkTypeResource> findByIds(Set<String> workTypeIds) {
        if (workTypeIds.isEmpty()) {
            return (root, query, builder) -> builder.conjunction();
        } else if (workTypeIds.size() == SINGLE_ELEMENT) {
            return (root, query, builder) -> builder.equal(
                root.get(WORK_TYPE_ID),
                workTypeIds.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid Entry"))
            );
        }
        return (root, query, builder) -> builder.in(root.get(WORK_TYPE_ID)).value(workTypeIds);
    }

}
