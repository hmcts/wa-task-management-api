package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;

class AssignmentTest {


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
            RoleCategory.STAFF,
            false,
            null,
            null,
            null,
            emptyMap(),
            emptyList()
        );

        assertEquals(id, assignment.getId());
        assertEquals(ActorIdType.IDAM, assignment.getActorIdType());
        assertEquals("someactorId", assignment.getActorId());
        assertEquals(RoleType.ORGANISATION, assignment.getRoleType());
        assertEquals("some-role-name", assignment.getRoleName());
        assertEquals(Classification.PUBLIC, assignment.getClassification());
        assertEquals(GrantType.SPECIFIC, assignment.getGrantType());
        assertEquals(RoleCategory.STAFF, assignment.getRoleCategory());
        assertEquals(false, assignment.isReadOnly());
        assertEquals(null, assignment.getBeginTime());
        assertEquals(null, assignment.getEndTime());
        assertEquals(null, assignment.getCreated());
        assertEquals(emptyMap(), assignment.getAttributes());
        assertEquals(emptyList(), assignment.getAuthorisations());

    }
}
