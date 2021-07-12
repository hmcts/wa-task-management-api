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

class RoleAssignmentTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = RoleAssignment.class;

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
        RoleAssignment roleAssignment = new RoleAssignment(
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

        Assertions.assertEquals(id, roleAssignment.getId());
        Assertions.assertEquals(ActorIdType.IDAM, roleAssignment.getActorIdType());
        Assertions.assertEquals("someactorId", roleAssignment.getActorId());
        Assertions.assertEquals(RoleType.ORGANISATION, roleAssignment.getRoleType());
        Assertions.assertEquals("some-role-name", roleAssignment.getRoleName());
        Assertions.assertEquals(Classification.PUBLIC, roleAssignment.getClassification());
        Assertions.assertEquals(GrantType.SPECIFIC, roleAssignment.getGrantType());
        Assertions.assertEquals(RoleCategory.LEGAL_OPERATIONS, roleAssignment.getRoleCategory());
        Assertions.assertFalse(roleAssignment.isReadOnly());
        Assertions.assertNull(roleAssignment.getBeginTime());
        Assertions.assertNull(roleAssignment.getEndTime());
        Assertions.assertNull(roleAssignment.getCreated());
        Assertions.assertEquals(emptyMap(), roleAssignment.getAttributes());
        Assertions.assertEquals(emptyList(), roleAssignment.getAuthorisations());

    }
}
