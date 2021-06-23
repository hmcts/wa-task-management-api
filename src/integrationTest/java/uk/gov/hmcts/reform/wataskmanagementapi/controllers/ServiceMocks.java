package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.GetRoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaExceptionMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ServiceMocks {

    private static final String IDAM_USER_ID = "IDAM_USER_ID";
    private ObjectMapper objectMapper = new ObjectMapper();

    private IdamWebApi idamWebApi;
    private CamundaServiceApi camundaServiceApi;
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    public ServiceMocks(IdamWebApi idamWebApi,
                        CamundaServiceApi camundaServiceApi,
                        RoleAssignmentServiceApi roleAssignmentServiceApi) {
        this.idamWebApi = idamWebApi;
        this.camundaServiceApi = camundaServiceApi;
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
    }

    public void mockServiceAPIs() {
        var userToken = "user_token";

        mockUserInfo();
        mockRoleAssignments(roleAssignmentServiceApi);

        when(idamWebApi.token(any())).thenReturn(new Token(userToken, "scope"));

        mockVariables();
    }

    public String createCamundaTestException(String type, String message) {
        try {
            return objectMapper.writeValueAsString(new CamundaExceptionMessage(type, message));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void mockRoleAssignments(RoleAssignmentServiceApi roleAssignmentServiceApi) {
        List<Assignment> assignments = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );
        GetRoleAssignmentResponse accessControlResponse = new GetRoleAssignmentResponse(
            assignments,
                links);
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
                    UUID.randomUUID().toString(), "tribunal-caseworker",
                    roleType,
                    roleClassification,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));
        return allTestRoles;
    }

    protected Assignment createBaseAssignment(String actorId,
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
            RoleCategory.LEGAL_OPERATIONS,
            false,
            attributes
        );
    }

    protected void mockUserInfo() {
        UserInfo mockedUserInfo = UserInfo.builder().uid(IDAM_USER_ID).build();
        when(idamWebApi.userInfo(any())).thenReturn(mockedUserInfo);
    }

    protected void mockVariables() {
        Map<String, CamundaVariable> processVariables = new ConcurrentHashMap<>();

        processVariables.put("tribunal-caseworker", new CamundaVariable("Read,Refer,Own,Manager,Cancel", "string"));
        processVariables.put("securityClassification", new CamundaVariable("PUBLIC", "string"));

        when(camundaServiceApi.getVariables(any(), any()))
            .thenReturn(processVariables);
    }

    protected CamundaTask getCamundaTask(String processInstanceId, String id) {
        return new CamundaTask(
            id,
            "some-name",
            "some-assignee",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "some-description",
            "some-owner",
            "formKey",
            processInstanceId
        );
    }

    protected List<Assignment> createRoleAssignmentsWithSCSSandIA() {
        List<Assignment> allTestRoles = new ArrayList<>();
        // Role Assignment with IA and RoleType Organisation
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        final Assignment orgAssignment = createBaseAssignment(
            UUID.randomUUID().toString(),
            "tribunal-caseworker",
            RoleType.ORGANISATION,
            Classification.PUBLIC,
            roleAttributes
        );
        allTestRoles.add(orgAssignment);

        // Role Assignment with SCSS and RoleType CASE
        roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "SSCS");
        roleAttributes.put(RoleAttributeDefinition.CASE_ID.value(), "caseId1");
        final Assignment caseAssignment = createBaseAssignment(
            UUID.randomUUID().toString(),
            "tribunal-caseworker",
            RoleType.CASE,
            Classification.PUBLIC,
            roleAttributes
        );
        allTestRoles.add(caseAssignment);

        return allTestRoles;
    }

}
