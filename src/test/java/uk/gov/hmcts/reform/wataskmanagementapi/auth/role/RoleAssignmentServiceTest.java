package uk.gov.hmcts.reform.wataskmanagementapi.auth.role;

import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request.MultipleQueryRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.ArrayMatching.arrayContaining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    private static final String IDAM_USER_TOKEN = "IDAM_USER_TOKEN";
    private static final String S2S_TOKEN = "S2S_SERVICE_TOKEN";

    @Mock
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private IdamTokenGenerator idamTokenGenerator;

    private RoleAssignmentService roleAssignmentService;
    @Captor
    private ArgumentCaptor<MultipleQueryRequest> captor;
    
    private List<Classification> classifications;

    @BeforeEach
    void setUp() {
        roleAssignmentService = new RoleAssignmentService(roleAssignmentServiceApi,
            authTokenGenerator, idamTokenGenerator);

        lenient().when(idamTokenGenerator.generate()).thenReturn(IDAM_USER_TOKEN);
        lenient().when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
    }

    @Test
    void testGetRolesForUser() {
        final RoleAssignmentResource mockRoleAssignmentResource = mock(RoleAssignmentResource.class);
        String idamUserId = "user";
        String authToken = "token";
        String serviceAuthToken = "authToken";
        when(authTokenGenerator.generate()).thenReturn(serviceAuthToken);
        when(roleAssignmentServiceApi.getRolesForUser(idamUserId, authToken, serviceAuthToken))
            .thenReturn(mockRoleAssignmentResource);

        List<RoleAssignment> createdRolesForUser = getRoleAssignmentList();
        when(mockRoleAssignmentResource.getRoleAssignmentResponse()).thenReturn(createdRolesForUser);

        List<RoleAssignment> rolesForUser = roleAssignmentService.getRolesForUser(idamUserId, authToken);
        assertEquals(createdRolesForUser, rolesForUser);
        verify(mockRoleAssignmentResource, times(1)).getRoleAssignmentResponse();
    }

    @Test
    void testGetRolesForUserThrowsUnauthorizedException() {
        String idamUserId = "user";
        String authToken = "token";
        String serviceAuthToken = "authToken";
        when(authTokenGenerator.generate()).thenReturn(serviceAuthToken);
        when(roleAssignmentServiceApi.getRolesForUser(idamUserId, authToken, serviceAuthToken))
            .thenThrow(FeignException.class);

        assertThrows(UnAuthorizedException.class,
            () -> roleAssignmentService.getRolesForUser(idamUserId, authToken));
    }

    @Test
    void testGetRolesForUserThrowsNullPointerWhenIdamUserIdIsNull() {
        String authToken = "token";

        assertThrows(NullPointerException.class,
            () -> roleAssignmentService.getRolesForUser(null, authToken));
    }

    @NotNull
    private List<RoleAssignment> getRoleAssignmentList() {
        List<RoleAssignment> createdRolesForUser = new ArrayList<>();
        createdRolesForUser.add(getRoleAssignment());
        return createdRolesForUser;
    }
    

    @ParameterizedTest
    @CsvSource({
        "PUBLIC, PUBLIC PRIVATE RESTRICTED",
        "PRIVATE, PRIVATE RESTRICTED",
        "RESTRICTED, RESTRICTED"
    })
    void should_return_query_request_classifications_according_to_securityClassification(
        String securityClassificationInput, String classificationInput) {

        classifications = Arrays.stream(classificationInput.split(" "))
            .map(Classification::valueOf)
            .collect(Collectors.toList());

        SecurityClassification securityClassification = SecurityClassification.valueOf(securityClassificationInput);
        TaskResource taskResource = createTestTaskWithRoleResources(
            securityClassification,
            singleton(taskRoleResource("tribunal-caseworker", true))
        );

        RoleAssignment roleAssignment = getRoleAssignment(Classification.valueOf(securityClassificationInput));

        final RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(
            singletonList(roleAssignment)
        );

        when(roleAssignmentServiceApi.queryRoleAssignments(eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            any(MultipleQueryRequest.class)))
            .thenReturn(roleAssignmentResource);

        List<RoleAssignment> roleAssignments = roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource);

        assertNotNull(roleAssignments);

        verify(roleAssignmentServiceApi).queryRoleAssignments(
            eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            captor.capture()
        );

        MultipleQueryRequest queryRequests = captor.getValue();

        assertThat(queryRequests).isNotNull();
        assertThat(queryRequests.getQueryRequests()).isNotEmpty();
        assertNotNull(queryRequests.getQueryRequests().get(0).getClassification());

        assertThat(classifications)
            .hasSameSizeAs(queryRequests.getQueryRequests().get(0).getClassification());

        arrayContaining(
            classifications,
            equalTo(queryRequests.getQueryRequests().get(0).getClassification())
        );
    }

    @ParameterizedTest
    @CsvSource({
        "PUBLIC, PUBLIC PRIVATE RESTRICTED",
        "PRIVATE, PRIVATE RESTRICTED",
        "RESTRICTED, RESTRICTED"
    })
    void should_return_query_request_classifications_when_auto_assignable_and_own_permissions_false(
        String securityClassificationInput, String classificationInput) {

        classifications = Arrays.stream(classificationInput.split(" "))
            .map(Classification::valueOf)
            .collect(Collectors.toList());

        SecurityClassification securityClassification = SecurityClassification.valueOf(securityClassificationInput);
        TaskRoleResource taskRoleResource = taskRoleResource("tribunal-caseworker", false);
        taskRoleResource.setOwn(false);
        TaskResource taskResource = createTestTaskWithRoleResources(
            securityClassification,
            singleton(taskRoleResource)
        );

        RoleAssignment roleAssignment = getRoleAssignment(Classification.valueOf(securityClassificationInput));

        final RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(
            singletonList(roleAssignment)
        );

        when(roleAssignmentServiceApi.queryRoleAssignments(eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            any(MultipleQueryRequest.class)))
            .thenReturn(roleAssignmentResource);

        List<RoleAssignment> roleAssignments = roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource);

        assertNotNull(roleAssignments);

        verify(roleAssignmentServiceApi).queryRoleAssignments(
            eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            captor.capture()
        );

        MultipleQueryRequest queryRequests = captor.getValue();

        assertThat(queryRequests).isNotNull();
        assertThat(queryRequests.getQueryRequests()).isNotEmpty();
        assertNotNull(queryRequests.getQueryRequests().get(0).getClassification());

        assertThat(classifications)
            .hasSameSizeAs(queryRequests.getQueryRequests().get(0).getClassification());

        arrayContaining(
            classifications,
            equalTo(queryRequests.getQueryRequests().get(0).getClassification())
        );
    }

    @ParameterizedTest
    @CsvSource({
        "PUBLIC, PUBLIC PRIVATE RESTRICTED",
        "PRIVATE, PRIVATE RESTRICTED",
        "RESTRICTED, RESTRICTED"
    })
    void should_return_query_request_classifications_when_auto_assignable_true_and_own_permissions_false(
        String securityClassificationInput, String classificationInput) {

        classifications = Arrays.stream(classificationInput.split(" "))
            .map(Classification::valueOf)
            .collect(Collectors.toList());

        SecurityClassification securityClassification = SecurityClassification.valueOf(securityClassificationInput);
        TaskRoleResource taskRoleResource = taskRoleResource("tribunal-caseworker", true);
        taskRoleResource.setOwn(false);
        TaskResource taskResource = createTestTaskWithRoleResources(
            securityClassification,
            singleton(taskRoleResource)
        );

        RoleAssignment roleAssignment = getRoleAssignment(Classification.valueOf(securityClassificationInput));

        final RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(
            singletonList(roleAssignment)
        );

        when(roleAssignmentServiceApi.queryRoleAssignments(eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            any(MultipleQueryRequest.class)))
            .thenReturn(roleAssignmentResource);

        List<RoleAssignment> roleAssignments = roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource);

        assertNotNull(roleAssignments);

        verify(roleAssignmentServiceApi).queryRoleAssignments(
            eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            captor.capture()
        );

        MultipleQueryRequest queryRequests = captor.getValue();

        assertThat(queryRequests).isNotNull();
        assertThat(queryRequests.getQueryRequests()).isNotEmpty();
        assertNotNull(queryRequests.getQueryRequests().get(0).getClassification());

        assertThat(classifications)
            .hasSameSizeAs(queryRequests.getQueryRequests().get(0).getClassification());

        arrayContaining(
            classifications,
            equalTo(queryRequests.getQueryRequests().get(0).getClassification())
        );
    }

    @ParameterizedTest
    @CsvSource({
        "PUBLIC, PUBLIC PRIVATE RESTRICTED",
        "PRIVATE, PRIVATE RESTRICTED",
        "RESTRICTED, RESTRICTED"
    })
    void should_return_query_request_classifications_when_auto_assignable_false_and_own_permissions_true(
        String securityClassificationInput, String classificationInput) {

        classifications = Arrays.stream(classificationInput.split(" "))
            .map(Classification::valueOf)
            .collect(Collectors.toList());

        SecurityClassification securityClassification = SecurityClassification.valueOf(securityClassificationInput);
        TaskRoleResource taskRoleResource = taskRoleResource("tribunal-caseworker", true);
        TaskResource taskResource = createTestTaskWithRoleResources(
            securityClassification,
            singleton(taskRoleResource)
        );

        RoleAssignment roleAssignment = getRoleAssignment(Classification.valueOf(securityClassificationInput));

        final RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(
            singletonList(roleAssignment)
        );

        when(roleAssignmentServiceApi.queryRoleAssignments(eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            any(MultipleQueryRequest.class)))
            .thenReturn(roleAssignmentResource);

        List<RoleAssignment> roleAssignments = roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource);

        assertNotNull(roleAssignments);

        verify(roleAssignmentServiceApi).queryRoleAssignments(
            eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            captor.capture()
        );

        MultipleQueryRequest queryRequests = captor.getValue();

        assertThat(queryRequests).isNotNull();
        assertThat(queryRequests.getQueryRequests()).isNotEmpty();
        assertNotNull(queryRequests.getQueryRequests().get(0).getClassification());

        assertThat(classifications)
            .hasSameSizeAs(queryRequests.getQueryRequests().get(0).getClassification());

        arrayContaining(
            classifications,
            equalTo(queryRequests.getQueryRequests().get(0).getClassification())
        );
    }

    @ParameterizedTest
    @CsvSource({
        "PUBLIC, PUBLIC PRIVATE RESTRICTED",
        "PRIVATE, PRIVATE RESTRICTED",
        "RESTRICTED, RESTRICTED"
    })
    void should_throw_server_error_exception_when_query_for_roles(
        String securityClassificationInput, String classificationInput) {

        classifications = Arrays.stream(classificationInput.split(" "))
            .map(Classification::valueOf)
            .collect(Collectors.toList());

        SecurityClassification securityClassification = SecurityClassification.valueOf(securityClassificationInput);
        TaskRoleResource taskRoleResource = taskRoleResource("tribunal-caseworker", true);
        TaskResource taskResource = createTestTaskWithRoleResources(
            securityClassification,
            singleton(taskRoleResource)
        );

        when(roleAssignmentServiceApi.queryRoleAssignments(eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            any(MultipleQueryRequest.class)))
            .thenThrow(FeignException.class);

        assertThrows(ServerErrorException.class,
            () -> roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource));

        verify(roleAssignmentServiceApi).queryRoleAssignments(
            eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            captor.capture()
        );

        MultipleQueryRequest queryRequests = captor.getValue();

        assertThat(queryRequests).isNotNull();
        assertThat(queryRequests.getQueryRequests()).isNotEmpty();
        assertNotNull(queryRequests.getQueryRequests().get(0).getClassification());

        assertThat(classifications)
            .hasSameSizeAs(queryRequests.getQueryRequests().get(0).getClassification());

        arrayContaining(
            classifications,
            equalTo(queryRequests.getQueryRequests().get(0).getClassification())
        );
    }

    private RoleAssignment getRoleAssignment() {
        return RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("DIVORCE", "373"))
            .grantType(GrantType.CHALLENGED)
            .attributes(emptyMap())
            .build();
    }

    private RoleAssignment getRoleAssignment(Classification classification) {
        final String testUserId = UUID.randomUUID().toString();
        return RoleAssignment.builder()
            .id("someId")
            .actorIdType(ActorIdType.IDAM)
            .actorId(testUserId)
            .roleName("tribunal-caseworker")
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(classification)
            .build();
    }

    private TaskResource createTestTaskWithRoleResources(
        SecurityClassification classification,
        Set<TaskRoleResource> taskResourceList) {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNCONFIGURED,
            "someCaseId",
            taskResourceList
        );
        taskResource.setSecurityClassification(classification);
        return taskResource;
    }

    private TaskRoleResource taskRoleResource(String name, boolean autoAssign) {
        return new TaskRoleResource(
            name,
            true,
            true,
            false,
            true,
            true,
            true,
            new String[]{"IA"},
            0,
            autoAssign
        );
    }
    
}
