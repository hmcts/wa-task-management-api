package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class AssignmentTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = Assignment.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }

    @Test
    void should_set_properties() {
        String id = UUID.randomUUID().toString();
        Assignment assignment = new Assignment(
            id,
            ActorIdType.IDAM,
            "someactorId",
            RoleType.ORGANISATION,
            "some-role-name",
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            null,
            null,
            null,
            emptyMap(),
            emptyList()
        );

        Assertions.assertEquals(id, assignment.getId());
        Assertions.assertEquals(ActorIdType.IDAM, assignment.getActorIdType());
        Assertions.assertEquals("someactorId", assignment.getActorId());
        Assertions.assertEquals(RoleType.ORGANISATION, assignment.getRoleType());
        Assertions.assertEquals("some-role-name", assignment.getRoleName());
        Assertions.assertEquals(Classification.PUBLIC, assignment.getClassification());
        Assertions.assertEquals(GrantType.SPECIFIC, assignment.getGrantType());
        Assertions.assertEquals(RoleCategory.LEGAL_OPERATIONS, assignment.getRoleCategory());
        Assertions.assertFalse(assignment.isReadOnly());
        Assertions.assertNull(assignment.getBeginTime());
        Assertions.assertNull(assignment.getEndTime());
        Assertions.assertNull(assignment.getCreated());
        Assertions.assertEquals(emptyMap(), assignment.getAttributes());
        Assertions.assertEquals(emptyList(), assignment.getAuthorisations());

    }
}
