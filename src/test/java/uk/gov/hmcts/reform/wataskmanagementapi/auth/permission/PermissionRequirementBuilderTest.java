package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder.ALREADY_INITIATED_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder.EMPTY_PERMISSION_TYPES_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder.NOT_INITIATED_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder.NOT_JOINED_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder.NULL_PERMISSION_JOIN_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder.NULL_PERMISSION_TYPES_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

public class PermissionRequirementBuilderTest {

    @Test
    public void permission_requirement_should_initiated_first() {
        PermissionRequirementBuilder builder = new PermissionRequirementBuilder();
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            builder.joinPermissionRequirement(PermissionJoin.AND);
        });
        assertEquals(exception.getMessage(), NOT_INITIATED_ERROR);

        exception = assertThrows(IllegalStateException.class, () -> {
            builder.nextPermissionRequirement(List.of(), PermissionJoin.AND);
        });
        assertEquals(exception.getMessage(), NOT_INITIATED_ERROR);
    }

    @Test
    public void permission_requirement_should_initiated_only_once() {
        PermissionRequirementBuilder builder = new PermissionRequirementBuilder();
        builder.initPermissionRequirement(List.of(OWN, EXECUTE), PermissionJoin.AND);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            builder.initPermissionRequirement(List.of(OWN, EXECUTE), PermissionJoin.AND);
        });
        assertEquals(exception.getMessage(), ALREADY_INITIATED_ERROR);
    }

    @Test
    public void permission_requirement_should_join_before_adding_next() {
        PermissionRequirementBuilder builder = new PermissionRequirementBuilder();
        builder.initPermissionRequirement(List.of(OWN, EXECUTE), PermissionJoin.AND);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            builder.nextPermissionRequirement(List.of(MANAGE), PermissionJoin.AND);
        });
        assertEquals(exception.getMessage(), NOT_JOINED_ERROR);
    }

    @Test
    public void should_initiate_with_valid_permission_requirement() {
        PermissionRequirementBuilder builder =  new PermissionRequirementBuilder();

        Exception exception = assertThrows(NullPointerException.class, () -> {
            builder.initPermissionRequirement(null, PermissionJoin.AND);
        });

        assertEquals(exception.getMessage(), NULL_PERMISSION_TYPES_ERROR);

        exception = assertThrows(IllegalArgumentException.class, () -> {
            builder.initPermissionRequirement(List.of(), PermissionJoin.AND);
        });

        assertEquals(exception.getMessage(), EMPTY_PERMISSION_TYPES_ERROR);

        exception = assertThrows(NullPointerException.class, () -> {
            builder.initPermissionRequirement(List.of(MANAGE), null);
        });

        assertEquals(exception.getMessage(), NULL_PERMISSION_JOIN_ERROR);
    }

    @Test
    public void should_add_valid_next_permission_requirement() {
        PermissionRequirementBuilder builder =  new PermissionRequirementBuilder()
            .initPermissionRequirement(List.of(OWN, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.AND);

        Exception exception = assertThrows(NullPointerException.class, () -> {
            builder.nextPermissionRequirement(null, PermissionJoin.AND);
        });

        assertEquals(exception.getMessage(), NULL_PERMISSION_TYPES_ERROR);

        exception = assertThrows(IllegalArgumentException.class, () -> {
            builder.nextPermissionRequirement(List.of(), PermissionJoin.AND);
        });

        assertEquals(exception.getMessage(), EMPTY_PERMISSION_TYPES_ERROR);

        exception = assertThrows(NullPointerException.class, () -> {
            builder.nextPermissionRequirement(List.of(MANAGE), null);
        });

        assertEquals(exception.getMessage(), NULL_PERMISSION_JOIN_ERROR);
    }

    @Test
    public void should_build_permission_requirement_collection() {
        PermissionRequirements requirements = new PermissionRequirementBuilder()
            .initPermissionRequirement(List.of(CLAIM, OWN), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(List.of(CLAIM, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(List.of(ASSIGN, OWN), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(List.of(CLAIM, EXECUTE), PermissionJoin.AND)
            .build();

        assertEquals(List.of(CLAIM, OWN), requirements.getPermissionRequirement().getPermissionTypes());
        assertEquals(PermissionJoin.AND, requirements.getPermissionRequirement().getPermissionJoin());
        assertEquals(PermissionJoin.OR, requirements.getPermissionJoin());

        PermissionRequirements nextRequirements = requirements.getNextPermissionRequirements();
        assertEquals(List.of(CLAIM, EXECUTE), nextRequirements.getPermissionRequirement().getPermissionTypes());
        assertEquals(PermissionJoin.AND, nextRequirements.getPermissionRequirement().getPermissionJoin());
        assertEquals(PermissionJoin.OR, nextRequirements.getPermissionJoin());

        nextRequirements = nextRequirements.getNextPermissionRequirements();
        assertEquals(List.of(ASSIGN, OWN), nextRequirements.getPermissionRequirement().getPermissionTypes());
        assertEquals(PermissionJoin.AND, nextRequirements.getPermissionRequirement().getPermissionJoin());
        assertEquals(PermissionJoin.OR, nextRequirements.getPermissionJoin());

        nextRequirements = nextRequirements.getNextPermissionRequirements();
        assertEquals(List.of(CLAIM, EXECUTE), nextRequirements.getPermissionRequirement().getPermissionTypes());
        assertEquals(PermissionJoin.AND, nextRequirements.getPermissionRequirement().getPermissionJoin());
        assertNull(nextRequirements.getPermissionJoin());
        assertNull(nextRequirements.getNextPermissionRequirements());
    }

    @Test
    public void should_build_permission_requirement_for_single_type() {
        PermissionRequirements requirements = new PermissionRequirementBuilder().buildSingleType(CLAIM);
        assertEquals(List.of(CLAIM), requirements.getPermissionRequirement().getPermissionTypes());
        assertEquals(PermissionJoin.OR, requirements.getPermissionRequirement().getPermissionJoin());
        assertNull(requirements.getPermissionJoin());
        assertNull(requirements.getNextPermissionRequirements());
    }

    @Test
    public void should_build_single_permission_requirement_with_and() {
        PermissionRequirements requirements = new PermissionRequirementBuilder()
            .buildSingleRequirementWithAnd(CLAIM, EXECUTE);
        assertEquals(List.of(CLAIM, EXECUTE), requirements.getPermissionRequirement().getPermissionTypes());
        assertEquals(PermissionJoin.AND, requirements.getPermissionRequirement().getPermissionJoin());
        assertNull(requirements.getPermissionJoin());
        assertNull(requirements.getNextPermissionRequirements());
    }

    @Test
    public void should_build_single_permission_requirement_with_or() {
        PermissionRequirements requirements = new PermissionRequirementBuilder()
            .buildSingleRequirementWithOr(CLAIM, EXECUTE);
        assertEquals(List.of(CLAIM, EXECUTE), requirements.getPermissionRequirement().getPermissionTypes());
        assertEquals(PermissionJoin.OR, requirements.getPermissionRequirement().getPermissionJoin());
        assertNull(requirements.getPermissionJoin());
        assertNull(requirements.getNextPermissionRequirements());
    }

}
