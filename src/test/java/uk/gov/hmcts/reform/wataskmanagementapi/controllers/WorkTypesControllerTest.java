package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetWorkTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.WorkTypesService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class WorkTypesControllerTest {

    private static final String IDAM_AUTH_TOKEN = "IDAM_AUTH_TOKEN";
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private WorkTypesService workTypesService;

    @Mock
    private AccessControlResponse mockedAccessControlResponse;

    private WorkTypesController workTypesController;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        workTypesController = new WorkTypesController(
            accessControlService,
            workTypesService
        );

        userInfo = UserInfo.builder()
            .roles(new ArrayList<>(asList("Role1", "Role2")))
            .build();
    }

    @Test
    void should_return_a_single_element_list_of_work_type_when_user_has_a_single_work_type() {

        RoleAssignment roleAssignment = new RoleAssignment(ActorIdType.IDAM,
            "1258555",
            RoleType.CASE,
            "Judge",
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.JUDICIAL,
            false,
            Map.of("workTypes", "hearing_work"));
        List<RoleAssignment> roleAssignmentList = singletonList(roleAssignment);
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignmentList);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(accessControlResponse);

        WorkType workType = new WorkType("hearing_work", "Hearing Work");

        when(workTypesService.getWorkTypes(accessControlResponse))
            .thenReturn(singletonList(workType));

        ResponseEntity<GetWorkTypesResponse> response = workTypesController.getWorkTypes(IDAM_AUTH_TOKEN, true);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), instanceOf(GetWorkTypesResponse.class));
        assertNotNull(response.getBody());

        List<WorkType> workTypeList = singletonList(workType);
        assertEquals(response.getBody(), new GetWorkTypesResponse(workTypeList));
        verify(workTypesService, times(1)).getWorkTypes(accessControlResponse);
    }

    @Test
    void should_return_a_list_of_work_types_when_user_has_many_work_types() {

        String workTypeKey = RoleAttributeDefinition.WORK_TYPES.value();
        RoleAssignment roleAssignment = new RoleAssignment(ActorIdType.IDAM,
            "1258555",
            RoleType.CASE,
            "Judge",
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.JUDICIAL,
            false,
            Map.of(workTypeKey, "hearing_work,upper_tribunal"));
        List<RoleAssignment> roleAssignmentList = singletonList(roleAssignment);
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignmentList);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(accessControlResponse);

        WorkType workType = new WorkType("hearing_work", "Hearing Work");
        WorkType workType2 = new WorkType("upper_tribunal", "Upper Tribunal");

        when(workTypesService.getWorkTypes(
            accessControlResponse
        ))
            .thenReturn(asList(workType, workType2));

        ResponseEntity<GetWorkTypesResponse> response = workTypesController.getWorkTypes(IDAM_AUTH_TOKEN, true);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), instanceOf(GetWorkTypesResponse.class));
        assertNotNull(response.getBody());

        List<WorkType> workTypeList = asList(workType, workType2);

        assertEquals(response.getBody(), new GetWorkTypesResponse(workTypeList));
        verify(workTypesService, times(1))
            .getWorkTypes(accessControlResponse);
    }

    @Test
    void should_return_empty_list_when_user_work_types_is_null() {
        UserInfo userInfo = new UserInfo("", "",
            new ArrayList<>(Arrays.asList("Role1", "Role2")),
            "",
            "",
            "");

        Map<String, String> workTypesMap = new HashMap<>();
        workTypesMap.put("workTypes", null);
        RoleAssignment roleAssignment = new RoleAssignment(ActorIdType.IDAM,
            "1258555",
            RoleType.CASE,
            "Judge",
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.JUDICIAL,
            false,
            workTypesMap);
        List<RoleAssignment> roleAssignmentList = Arrays.asList(roleAssignment);
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignmentList);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(accessControlResponse);

        ResponseEntity<GetWorkTypesResponse> response = workTypesController.getWorkTypes(
            IDAM_AUTH_TOKEN, true
        );
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getWorkTypes());
        assertEquals(emptyList(), response.getBody().getWorkTypes());
        verify(workTypesService, times(1)).getWorkTypes(any());
    }

    @Test
    void should_return_empty_list_when_user_does_not_have_work_types() {

        RoleAssignment roleAssignment = new RoleAssignment(ActorIdType.IDAM,
            "1258555",
            RoleType.CASE,
            "Judge",
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.JUDICIAL,
            false,
            new HashMap<>());
        List<RoleAssignment> roleAssignmentList = singletonList(roleAssignment);
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignmentList);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(accessControlResponse);

        ResponseEntity<GetWorkTypesResponse> response = workTypesController.getWorkTypes(IDAM_AUTH_TOKEN, true);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new GetWorkTypesResponse(emptyList()), response.getBody());
        verify(workTypesService, times(1)).getWorkTypes(any());
    }

    @Test
    void should_return_empty_list_when_role_assigment_is_empty() {

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(mockedAccessControlResponse);

        ResponseEntity<GetWorkTypesResponse> response = workTypesController.getWorkTypes(IDAM_AUTH_TOKEN, true);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new GetWorkTypesResponse(emptyList()), response.getBody());
        verify(workTypesService, times(1)).getWorkTypes(any());
    }
}
