package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WorkType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkTypesServiceTest {

    @Mock
    private CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;
    private WorkTypesService workTypesService;

    @BeforeEach
    public void setUp() {
        workTypesService = new WorkTypesService(
            cftWorkTypeDatabaseService
        );
    }

    @Test
    void should_return_empty_list_if_role_assignments_is_empty() {

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, emptyList());
        List<WorkType> response = workTypesService.getWorkTypes(accessControlResponse);

        assertNotNull(response);
        assertEquals(emptyList(), response);
    }

    @Test
    void should_return_empty_list_if_no_actor_work_types() {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");

        List<RoleAssignment> allTestRoles = createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);


        AccessControlResponse accessControlResponse = new AccessControlResponse(null, allTestRoles);
        List<WorkType> response = workTypesService.getWorkTypes(accessControlResponse);

        assertNotNull(response);
        assertEquals(emptyList(), response);
    }

    @Test
    void should_return_empty_list_when_role_not_found_in_db() {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles = createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, allTestRoles);

        List<WorkType> response = workTypesService.getWorkTypes(accessControlResponse);

        assertNotNull(response);
        assertEquals(emptyList(), response);
    }

    @Test
    void should_return_all_work_types_from_role_assignment() {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles = createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, allTestRoles);
        when(cftWorkTypeDatabaseService.getWorkTypes(Set.of("upper_tribunal", "hearing_work")))
            .thenReturn(List.of(new WorkType("upper_tribunal", "Upper Tribunal"),
                new WorkType("hearing_work", "Hearing work")));

        List<WorkType> response = workTypesService.getWorkTypes(accessControlResponse);


        WorkType expectedWorkType1 = new WorkType("upper_tribunal", "Upper Tribunal");
        WorkType expectedWorkType2 = new WorkType("hearing_work", "Hearing work");

        List<WorkType> expectedResponse = asList(expectedWorkType1, expectedWorkType2);
        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    void should_return_no_work_types_from_role_assignment_when_null_attributes() {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        List<RoleAssignment> allTestRoles = createTestRoleAssignmentsWithRoleAttributes(roleNames, null);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, allTestRoles);

        List<WorkType> response = workTypesService.getWorkTypes(accessControlResponse);

        assertNotNull(response);
        assertEquals(emptyList(), response);

        verify(cftWorkTypeDatabaseService, never()).getWorkTypes(any());
    }

    @Test
    void should_return_no_work_types_from_role_assignment_when_no_attributes() {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();

        List<RoleAssignment> allTestRoles = createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, allTestRoles);

        List<WorkType> response = workTypesService.getWorkTypes(accessControlResponse);

        assertNotNull(response);
        assertEquals(emptyList(), response);

        verify(cftWorkTypeDatabaseService, never()).getWorkTypes(any());
    }

    @Test
    void should_return_all_work_types_from_role_assignment_using_trim() {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        String workTypes = "hearing_work, decision_making_work, applications";
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), workTypes);

        List<RoleAssignment> allTestRoles = createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, allTestRoles);
        when(cftWorkTypeDatabaseService.getWorkTypes(
            Set.of("hearing_work", "decision_making_work", "applications")
        )).thenReturn(List.of(
                new WorkType("hearing_work", "Hearing work"),
                new WorkType("decision_making_work", "Decision-making work"),
                new WorkType("applications", "Applications")
            )
        );

        List<WorkType> response = workTypesService.getWorkTypes(accessControlResponse);

        WorkType expectedWorkType1 = new WorkType("hearing_work", "Hearing work");
        WorkType expectedWorkType2 = new WorkType("decision_making_work", "Decision-making work");
        WorkType expectedWorkType3 = new WorkType("applications", "Applications");

        List<WorkType> expectedResponse = asList(expectedWorkType1, expectedWorkType2, expectedWorkType3);
        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    void should_return_all_work_types() {

        List<WorkType> expectedWorkTypes = List.of(
            new WorkType("hearing-work", "Hearing work"),
            new WorkType("upper-tribunal", "Upper Tribunal"),
            new WorkType("routine-work", "Routine work"),
            new WorkType("decision-making-work", "Decision-making work"),
            new WorkType("applications", "Applications"),
            new WorkType("priority", "Priority"),
            new WorkType("access-requests", "Access requests"),
            new WorkType("error-management", "Error management"));

        when(cftWorkTypeDatabaseService.getAllWorkTypes()).thenReturn(expectedWorkTypes);

        List<WorkType> actualWorkTypes = workTypesService.getAllWorkTypes();
        assertNotNull(expectedWorkTypes);
        assertEquals(expectedWorkTypes.size(), actualWorkTypes.size());
        assertThat(actualWorkTypes).isNotEmpty();
    }


    private List<RoleAssignment> createTestRoleAssignmentsWithRoleAttributes(List<String> roleNames,
                                                                             Map<String, String> roleAttributes) {

        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                RoleAssignment roleAssignment = createBaseAssignment(
                    UUID.randomUUID().toString(),
                    "tribunal-caseworker",
                    roleType,
                    Classification.PUBLIC,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));
        return allTestRoles;
    }

    private RoleAssignment createBaseAssignment(String actorId,
                                                String roleName,
                                                RoleType roleType,
                                                Classification classification,
                                                Map<String, String> attributes) {
        return new RoleAssignment(
            ActorIdType.IDAM,
            actorId,
            roleType,
            roleName,
            classification,
            GrantType.SPECIFIC,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            attributes
        );
    }
}
