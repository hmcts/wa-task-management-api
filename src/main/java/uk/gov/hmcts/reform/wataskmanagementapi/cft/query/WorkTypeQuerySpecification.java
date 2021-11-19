package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;

import java.util.Set;

public final class WorkTypeQuerySpecification {

    public static final String WORK_TYPE_ID = "id";

    private WorkTypeQuerySpecification() {
        // avoid creating object
    }

    public static Specification<WorkTypeResource> findByIds(Set<String> workTypeIds) {
        return (root, query, builder) -> builder.in(root.get(WORK_TYPE_ID)).value(workTypeIds);
    }

}
