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
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTWorkTypeDatabaseService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class WorkTypesControllerTest {

    private static final String IDAM_AUTH_TOKEN = "IDAM_AUTH_TOKEN";
    @Mock
    private CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;
    @Mock
    private AccessControlService accessControlService;

    @Mock
    private AccessControlResponse mockedAccessControlResponse;

    private WorkTypesController workTypesController;
    private String workTypeKey;

    @BeforeEach
    void setUp() {
        workTypesController = new WorkTypesController(
            cftWorkTypeDatabaseService,
            accessControlService
        );
    }

    @Test
    void should_return_all_work_types() {
        List<WorkType> workTypes = new ArrayList<>();
        workTypes.add(new WorkType("hearing_work","Hearing Work"));
        workTypes.add(new WorkType("tribunal_work","Tribunal Work"));


        when(cftWorkTypeDatabaseService.getAllWorkTypes()).thenReturn(workTypes);

        ResponseEntity<GetWorkTypesResponse<WorkType>> response = workTypesController.getWorkTypes(
            IDAM_AUTH_TOKEN, false
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        List<WorkType> expectedList = new ArrayList<>();
        expectedList.add(new WorkType("hearing_work","Hearing Work"));
        expectedList.add(new WorkType("tribunal_work","Tribunal Work"));

        assertEquals(expectedList, response.getBody().getWorkTypes());
        verify(cftWorkTypeDatabaseService, times(1))
            .getAllWorkTypes();
    }

    @Test
    void should_return_a_single_element_list_of_work_type_when_user_has_a_single_work_type() {

        UserInfo userInfo = new UserInfo("", "",
                                         new ArrayList<>(Arrays.asList("Role1","Role2")),
                                         "",
                                         "",
                                         "");

        RoleAssignment roleAssignment = new RoleAssignment(ActorIdType.IDAM,
                                                           "1258555",
                                                           RoleType.CASE,
                                                           "Judge",
                                                           Classification.PUBLIC,
                                                           GrantType.BASIC,
                                                           RoleCategory.JUDICIAL,
                                                           false,
                                                           Map.of("workTypes","hearing_work"));
        List<RoleAssignment> roleAssignmentList = Arrays.asList(roleAssignment);
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo,roleAssignmentList);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(accessControlResponse);

        WorkType workType = new WorkType("hearing_work","Hearing Work");

        when(cftWorkTypeDatabaseService.getWorkType(
            workType.getId()
        ))
            .thenReturn(Optional.of(workType));

        ResponseEntity<GetWorkTypesResponse<WorkType>> response = workTypesController.getWorkTypes(
            IDAM_AUTH_TOKEN, true
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        List<WorkType> workTypeList = Arrays.asList(workType);
        assertEquals(workTypeList, response.getBody().getWorkTypes());
        verify(cftWorkTypeDatabaseService, times(1))
            .getWorkType(workType.getId());
    }

    @Test
    void should_return_a_list_of_work_types_when_user_has_many_work_types() {

        UserInfo userInfo = UserInfo.builder()
            .email("")
            .uid("")
            .roles(new ArrayList<>(Arrays.asList("Role1", "Role2")))
            .name("")
            .givenName("")
            .familyName("")
            .build();

        workTypeKey = RoleAttributeDefinition.WORK_TYPES.value();
        RoleAssignment roleAssignment = new RoleAssignment(ActorIdType.IDAM,
                                                           "1258555",
                                                           RoleType.CASE,
                                                           "Judge",
                                                           Classification.PUBLIC,
                                                           GrantType.BASIC,
                                                           RoleCategory.JUDICIAL,
                                                           false,
                                                           Map.of(workTypeKey, "hearing_work,upper_tribunal"));
        List<RoleAssignment> roleAssignmentList = Arrays.asList(roleAssignment);
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo,roleAssignmentList);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(accessControlResponse);

        WorkType workType = new WorkType("hearing_work","Hearing Work");
        WorkType workType2 = new WorkType("upper_tribunal","Upper Tribunal");

        when(cftWorkTypeDatabaseService.getWorkType(
            workType.getId()
        ))
            .thenReturn(Optional.of(workType));

        when(cftWorkTypeDatabaseService.getWorkType(
            workType2.getId()
        ))
            .thenReturn(Optional.of(workType2));

        ResponseEntity<GetWorkTypesResponse<WorkType>> response = workTypesController.getWorkTypes(
            IDAM_AUTH_TOKEN, true
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getWorkTypes());

        List<WorkType> workTypeList = Arrays.asList(workType,workType2);

        assertEquals(workTypeList.size(), response.getBody().getWorkTypes().size());
        verify(cftWorkTypeDatabaseService, times(1))
            .getWorkType(workType.getId());

        verify(cftWorkTypeDatabaseService, times(1))
            .getWorkType(workType2.getId());
    }

    @Test
    void should_return_empty_list_when_user_work_types_is_empty() {
        UserInfo userInfo = new UserInfo("", "",
                                         new ArrayList<>(Arrays.asList("Role1","Role2")),
                                         "",
                                         "",
                                         "");

        RoleAssignment roleAssignment = new RoleAssignment(ActorIdType.IDAM,
                                                           "1258555",
                                                           RoleType.CASE,
                                                           "Judge",
                                                           Classification.PUBLIC,
                                                           GrantType.BASIC,
                                                           RoleCategory.JUDICIAL,
                                                           false,
            Map.of("workTypes",""));
        List<RoleAssignment> roleAssignmentList = Arrays.asList(roleAssignment);
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo,roleAssignmentList);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(accessControlResponse);

        ResponseEntity<GetWorkTypesResponse<WorkType>> response = workTypesController.getWorkTypes(
            IDAM_AUTH_TOKEN, true
        );
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getWorkTypes());
        assertEquals(emptyList(), response.getBody().getWorkTypes());
        verify(cftWorkTypeDatabaseService, times(0)).getWorkType(anyString());
    }

    @Test
    void should_return_empty_list_when_user_work_types_is_null() {
        UserInfo userInfo = new UserInfo("", "",
            new ArrayList<>(Arrays.asList("Role1","Role2")),
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
            GrantType.BASIC,
            RoleCategory.JUDICIAL,
            false,
            workTypesMap);
        List<RoleAssignment> roleAssignmentList = Arrays.asList(roleAssignment);
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo,roleAssignmentList);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(accessControlResponse);

        ResponseEntity<GetWorkTypesResponse<WorkType>> response = workTypesController.getWorkTypes(
            IDAM_AUTH_TOKEN, true
        );
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getWorkTypes());
        assertEquals(emptyList(), response.getBody().getWorkTypes());
        verify(cftWorkTypeDatabaseService, times(0)).getWorkType(anyString());
    }

    @Test
    void should_return_empty_list_when_role_assignment_is_empty() {

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(mockedAccessControlResponse);

        ResponseEntity<GetWorkTypesResponse<WorkType>> response = workTypesController.getWorkTypes(
            IDAM_AUTH_TOKEN, true
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getWorkTypes());
        assertEquals(emptyList(), response.getBody().getWorkTypes());
        verify(cftWorkTypeDatabaseService, times(0)).getWorkType(anyString());
    }
}
