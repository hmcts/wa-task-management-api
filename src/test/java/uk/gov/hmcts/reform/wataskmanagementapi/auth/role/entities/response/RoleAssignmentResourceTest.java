package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class RoleAssignmentResourceTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = RoleAssignmentResource.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }

    @Test
    void should_update_role_assignment_response() {
        RoleAssignmentResource resource = new RoleAssignmentResource();
        List<RoleAssignment> assignments = List.of(new RoleAssignment());

        resource.setRoleAssignmentResponse(assignments);

        assertThat(resource.getRoleAssignmentResponse()).isSameAs(assignments);
    }
}
