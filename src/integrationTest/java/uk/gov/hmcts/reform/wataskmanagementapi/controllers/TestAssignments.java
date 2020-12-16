package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.GetRoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestAssignments {

    public void mockRoleAssignments(RoleAssignmentServiceApi roleAssignmentServiceApi) {
        List<Assignment> assignments = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );
        GetRoleAssignmentResponse accessControlResponse = new GetRoleAssignmentResponse(
            assignments
        );
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);
    }

    private List<Assignment> createTestAssignments(List<String> roleNames,
                                                  Classification roleClassification,
                                                  Map<String, String> roleAttributes) {

        List<Assignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                         Assignment roleAssignment = createBaseAssignment(
                             UUID.randomUUID().toString(),
                             "tribunal-caseworker",
                             roleType,
                             roleClassification,
                             roleAttributes
                         );
                         allTestRoles.add(roleAssignment);
                     }
            ));
        return allTestRoles;
    }

    private Assignment createBaseAssignment(String actorId,
                                            String roleName,
                                            RoleType roleType,
                                            Classification classification,
                                            Map<String, String> attributes) {
        return new Assignment(
            ActorIdType.IDAM,
            actorId,
            roleType,
            roleName,
            classification,
            GrantType.SPECIFIC,
            RoleCategory.STAFF,
            false,
            attributes
        );
    }

    public void mockUserInfo(IdamServiceApi idamServiceApi) {
       var IDAM_USER_ID = "IDAM_USER_ID";
        UserInfo mockedUserInfo = UserInfo.builder().uid(IDAM_USER_ID).build();
        when(idamServiceApi.userInfo(any())).thenReturn(mockedUserInfo);
    }

    public void mockVariables(CamundaServiceApi camundaServiceApi) {
        Map<String, CamundaVariable> processVariables = new ConcurrentHashMap<>();

        processVariables.put("tribunal-caseworker", new CamundaVariable("Read,Refer,Own,Manager,Cancel", "string"));
        processVariables.put("securityClassification", new CamundaVariable("PUBLIC", "string"));

        when(camundaServiceApi.getVariables(any(), any()))
            .thenReturn(processVariables);
    }
}
