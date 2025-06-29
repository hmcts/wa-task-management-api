package uk.gov.hmcts.reform.wataskmanagementapi.services;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.NotesRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.data.RoleAssignmentCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskRolePermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.DatabaseConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.InvalidRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.ServiceMandatoryFieldValidationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin.OR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL_OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.COMPLETE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.COMPLETE_OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.REFER;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN_CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNCLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNCLAIM_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.TERMINATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.ROLE_ASSIGNMENT_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.TaskActionAttributesBuilder.setTaskActionAttributes;

@ExtendWith(MockitoExtension.class)
@Slf4j
class TaskManagementServiceUnitTest extends CamundaHelpers {

    public static final String A_TASK_TYPE = "followUpOverdueReasonsForAppeal";
    public static final String A_TASK_NAME = "follow Up Overdue Reasons For Appeal";
    public static final String SOME_ROLE_ASSIGNMENT_ID = "someRoleAssignmentId";
    public static final String IDAM_SYSTEM_USER = "IDAM_SYSTEM_USER";

    @Mock
    CamundaService camundaService;
    @Mock
    CamundaServiceApi camundaServiceApi;
    @Mock
    CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;
    @Spy
    CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    @Mock
    CftQueryService cftQueryService;

    @Mock
    CaseConfigurationProviderService caseConfigurationProviderService;
    @Mock
    AccessControlResponse accessControlResponse;

    RoleAssignmentVerificationService roleAssignmentVerification;
    TaskManagementService taskManagementService;
    String taskId;
    String caseId;
    @Mock
    private EntityManager entityManager;
    @Mock(extraInterfaces = Serializable.class)
    private CriteriaBuilder builder;
    @Mock
    private CriteriaQuery<TaskResource> criteriaQuery;

    @Mock
    private TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;
    @Mock
    private Predicate predicate;
    @Mock
    private CriteriaBuilder.In<Object> inObject;
    @Mock
    private CriteriaBuilder.In<Object> values;
    @Mock
    private Root<TaskResource> root;
    @Mock
    private Path<Object> path;
    @Mock
    private Path<Object> authorizations;
    @Mock
    private Join<Object, Object> taskRoleResources;
    @Mock
    private TypedQuery<TaskResource> query;
    @Captor
    private ArgumentCaptor<TaskResource> taskResourceCaptor;
    @Mock
    private IdamTokenGenerator idamTokenGenerator;
    @Mock
    private UserInfo userInfo;

    private Map<String, Object> requestParamMap = new HashMap<>();

    @Test
    void unclaimTask_succeed_when_task_assignee_differs_from_user_and_has_unassign_gp_flag_on() {

        userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        RoleAssignment roleAssignment1 = new RoleAssignment(
            ActorIdType.IDAM,
            IDAM_USER_ID,
            RoleType.ORGANISATION,
            "judge",
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.JUDICIAL,
            false,
            Map.of("workTypes", "hearing_work")
        );
        RoleAssignment roleAssignment2 = new RoleAssignment(
            ActorIdType.IDAM,
            IDAM_USER_ID,
            RoleType.ORGANISATION,
            "tribunal-caseworker",
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.JUDICIAL,
            false,
            Map.of("workTypes", "hearing_work")
        );
        List<RoleAssignment> roleAssignmentList = asList(roleAssignment1, roleAssignment2);

        TaskResource taskResource = spy(TaskResource.class);
        PermissionRequirements requirements
            = PermissionRequirementBuilder.builder().buildSingleRequirementWithOr(UNCLAIM, UNASSIGN);
        when(cftQueryService.getTask(taskId, roleAssignmentList, requirements))
            .thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

        when(taskResource.getState()).thenReturn(CFTTaskState.UNASSIGNED);
        when(taskResource.getAssignee()).thenReturn("wrongid");

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            false,
            false,
            false,
            false,
            false,
            new String[]{"SPECIFIC", "STANDARD"},
            0,
            false,
            "JUDICIAL",
            taskId,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            false,
            false,
            false
        );
        Set<TaskRoleResource> taskRoleResourcesItems = new HashSet<>();
        taskRoleResourcesItems.add(taskRoleResource);
        when(taskResource.getTaskRoleResources()).thenReturn(taskRoleResourcesItems);

        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentList);

        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);


        boolean taskHasUnassigned = taskResource.getState().getValue().equals(CFTTaskState.UNASSIGNED.getValue());
        taskManagementService.unclaimTask(taskId, accessControlResponse);
        verify(camundaService, times(1)).unclaimTask(taskId, taskHasUnassigned);
    }

    @Test
    void unclaimTask_throw_403_when_task_assignee_differs_from_user_and_no_unassign_gp_flag_on() {

        userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        RoleAssignment roleAssignment = new RoleAssignment(
            ActorIdType.IDAM,
            IDAM_USER_ID,
            RoleType.ORGANISATION,
            "tribunal-caseworker",
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.JUDICIAL,
            false,
            Map.of("workTypes", "hearing_work")
        );
        List<RoleAssignment> roleAssignmentList = singletonList(roleAssignment);

        TaskResource taskResource = spy(TaskResource.class);
        PermissionRequirements requirements
            = PermissionRequirementBuilder.builder().buildSingleRequirementWithOr(UNCLAIM, UNASSIGN);
        when(cftQueryService.getTask(taskId, roleAssignmentList, requirements))
            .thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

        when(taskResource.getState()).thenReturn(CFTTaskState.UNASSIGNED);
        when(taskResource.getAssignee()).thenReturn("wrongid");

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            false,
            false,
            false,
            false,
            false,
            new String[]{"SPECIFIC", "STANDARD"},
            0,
            false,
            "JUDICIAL",
            taskId,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        );
        Set<TaskRoleResource> taskRoleResourcesItems = new HashSet<>();
        taskRoleResourcesItems.add(taskRoleResource);
        when(taskResource.getTaskRoleResources()).thenReturn(taskRoleResourcesItems);

        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentList);

        assertThatThrownBy(() -> taskManagementService.unclaimTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: "
                        + "The request failed the Role Assignment checks performed.");

        verify(camundaService, times(0)).assignTask(any(), any(), anyBoolean());
    }

    @BeforeEach
    void setUp() {
        roleAssignmentVerification = new RoleAssignmentVerificationService(
            cftTaskDatabaseService,
            cftQueryService,
            cftSensitiveTaskEventLogsDatabaseService
        );

        taskManagementService = new TaskManagementService(
            camundaService,
            cftTaskDatabaseService,
            cftTaskMapper,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerification,
            entityManager,
            idamTokenGenerator,
            cftSensitiveTaskEventLogsDatabaseService,
            taskMandatoryFieldsValidator
        );


        taskId = UUID.randomUUID().toString();
        caseId = UUID.randomUUID().toString();

        lenient().when(entityManager.getCriteriaBuilder()).thenReturn(builder);
        lenient().when(builder.createQuery(TaskResource.class)).thenReturn(criteriaQuery);
        lenient().when(criteriaQuery.from(TaskResource.class)).thenReturn(root);
        lenient().when(criteriaQuery.distinct(true)).thenReturn(criteriaQuery);
        lenient().when(criteriaQuery.select(root)).thenReturn(criteriaQuery);
        lenient().when(entityManager.createQuery(criteriaQuery)).thenReturn(query);
        lenient().when(builder.in(any())).thenReturn(inObject);
        lenient().when(inObject.value(any())).thenReturn(values);
        lenient().when(builder.or(any(), any())).thenReturn(inObject);
        lenient().when(builder.or(any())).thenReturn(inObject);
        lenient().when(builder.and(any(), any())).thenReturn(inObject);
        lenient().when(builder.and(any(), any(), any(), any(), any(), any(), any())).thenReturn(inObject);

        lenient().when(builder.conjunction()).thenReturn(predicate);
        lenient().when(builder.equal(any(), any())).thenReturn(predicate);
        lenient().when(inObject.value(any())).thenReturn(values);

        lenient().when(taskRoleResources.get(anyString())).thenReturn(authorizations);

        lenient().when(authorizations.isNull()).thenReturn(predicate);
        lenient().when(root.join(anyString())).thenReturn(taskRoleResources);
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(root.get(anyString()).get(anyString())).thenReturn(path);

        lenient().when(caseConfigurationProviderService.evaluateConfigurationDmn(
            anyString(),
            anyMap()
        )).thenReturn(List.of(new ConfigurationDmnEvaluationResponse(
                CamundaValue.stringValue("caseName"),
                CamundaValue.stringValue("Value"),
                CamundaValue.booleanValue(true)
            )
        ));
        lenient().when(idamTokenGenerator.getUserInfo(any())).thenReturn(userInfo);
        lenient().when(userInfo.getUid()).thenReturn("IDAM_SYSTEM_USER");
    }

    @Nested
    @DisplayName("getTaskById()")
    class GetTaskById {
        @Test
        void getTaskById_should_succeed_and_return_an_optional_empty() {

            when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.empty());

            Optional<TaskResource> response = taskManagementService.getTaskById(taskId);

            assertFalse(response.isPresent());
        }

        @Test
        void getTaskById_should_succeed_and_return_an_optional_task() {

            TaskResource someTaskResource = mock(TaskResource.class);
            when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.of(someTaskResource));

            Optional<TaskResource> response = taskManagementService.getTaskById(taskId);

            assertTrue(response.isPresent());
            assertEquals(someTaskResource, response.get());
        }
    }

    @Nested
    @DisplayName("getTask()")
    class Release2EndpointsGetTask {
        @Test
        void getTask_should_succeed_and_return_mapped_task() {
            Task mockedMappedTask = mock(Task.class);
            when(accessControlResponse.getRoleAssignments())
                .thenReturn(singletonList(RoleAssignmentCreator.aRoleAssignment().build()));
            TaskResource taskResource = spy(TaskResource.class);
            PermissionRequirements requirements = PermissionRequirementBuilder.builder().buildSingleType(READ);
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.of(taskResource));

            doReturn(mockedMappedTask)
                .when(cftTaskMapper).mapToTaskWithPermissions(eq(taskResource), any());

            when(taskResource.getTaskRoleResources()).thenReturn(new TreeSet<TaskRoleResource>());

            Task response = taskManagementService.getTask(taskId, accessControlResponse);

            assertNotNull(response);
            assertEquals(mockedMappedTask, response);
            verify(camundaService, times(0)).getTaskVariables(any());
            verify(camundaService, times(0)).getMappedTask(any(), any());
            verifyNoInteractions(camundaService);
        }

        @Test
        void getTask_should_throw_task_not_found_exception_when_task_does_not_exist() {

            PermissionRequirementBuilder.builder().buildSingleType(READ);
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskManagementService.getTask(taskId, accessControlResponse))
                .isInstanceOf(TaskNotFoundException.class)
                .hasNoCause()
                .hasMessage("Task Not Found Error: The task could not be found.");
            verify(camundaService, times(0)).getTaskVariables(any());
            verify(camundaService, times(0)).getMappedTask(any(), any());
            verifyNoInteractions(camundaService);
        }

        @Test
        void getTask_should_throw_role_assignment_verification_exception_when_query_returns_empty_task() {
            PermissionRequirements requirements = PermissionRequirementBuilder.builder().buildSingleType(READ);

            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.empty());
            TaskResource taskResource = spy(TaskResource.class);
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            assertThatThrownBy(() -> taskManagementService.getTask(taskId, accessControlResponse))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");
            verify(camundaService, times(0)).getTaskVariables(any());
            verify(camundaService, times(0)).getMappedTask(any(), any());
            verifyNoInteractions(camundaService);
        }
    }

    @Nested
    @DisplayName("updateIndex()")
    class UpdateIndex {

        @Test
        void should_succeed() {
            TaskResource taskResource = new TaskResource(
                taskId, "taskName", "taskType", CFTTaskState.ASSIGNED
            );
            when(cftTaskDatabaseService.findByIdAndWaitAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(any())).thenReturn(taskResource);

            taskManagementService.updateTaskIndex(taskId);
            assertEquals(true, taskResource.getIndexed());
        }

        @Test
        void should_complete_for_null_task() {
            when(cftTaskDatabaseService.findByIdAndWaitAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskManagementService.updateTaskIndex(taskId))
                .isInstanceOf(TaskNotFoundException.class)
                .hasNoCause()
                .hasMessage("Task Not Found Error: The task could not be found.");
            verify(cftTaskDatabaseService, times(0)).saveTask(any());
        }

        @Test
        void should_complete_when_lock_time_out() {
            Logger logger = (Logger) LoggerFactory.getLogger(TaskManagementService.class);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();

            logger.addAppender(listAppender);

            when(cftTaskDatabaseService.findByIdAndWaitAndObtainPessimisticWriteLock(taskId))
                .thenThrow(new LockTimeoutException());
            verify(cftTaskDatabaseService, times(0)).saveTask(any());

            taskManagementService.updateTaskIndex(taskId);

            List<String> logsList = List.copyOf(listAppender.list)
                .stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
            assertTrue(logsList.contains("PersistenceException occurred in updating indexed field of taskId:"
                                         + taskId));
        }
    }

    @Nested
    @DisplayName("claimTask()")
    class Release2EndpointsClaimTask {

        @Test
        void claimTask_should_succeed() {
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(Optional.of(taskResource));

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            taskManagementService.claimTask(taskId, accessControlResponse);

            verify(camundaService, times(1)).assignTask(taskId, IDAM_USER_ID, false);

        }

        @Test
        void claimTask_should_succeed_for_granular_permission() {
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(asList(CLAIM, OWN), PermissionJoin.AND)
                .joinPermissionRequirement(PermissionJoin.OR)
                .nextPermissionRequirement(asList(CLAIM, EXECUTE), PermissionJoin.AND)
                .joinPermissionRequirement(PermissionJoin.OR)
                .nextPermissionRequirement(asList(ASSIGN, EXECUTE), PermissionJoin.AND)
                .joinPermissionRequirement(PermissionJoin.OR)
                .nextPermissionRequirement(asList(ASSIGN, OWN), PermissionJoin.AND)
                .build();
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            taskManagementService.claimTask(taskId, accessControlResponse);

            verify(camundaService, times(1)).assignTask(taskId, IDAM_USER_ID, false);

        }

        @Test
        void claimTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

            RoleAssignment roleAssignment = mock(RoleAssignment.class);
            when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
            List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(Optional.empty());
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            assertThatThrownBy(() -> taskManagementService.claimTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

            verify(camundaService, times(0)).claimTask(any(), any());
        }

        @Test
        void claimTask_should_throw_role_assignment_verification_exception_when_granular_permission_access_fail() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(asList(CLAIM, OWN), PermissionJoin.AND)
                .joinPermissionRequirement(PermissionJoin.OR)
                .nextPermissionRequirement(asList(CLAIM, EXECUTE), PermissionJoin.AND)
                .joinPermissionRequirement(PermissionJoin.OR)
                .nextPermissionRequirement(asList(ASSIGN, EXECUTE), PermissionJoin.AND)
                .joinPermissionRequirement(PermissionJoin.OR)
                .nextPermissionRequirement(asList(ASSIGN, OWN), PermissionJoin.AND)
                .build();
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.empty());
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            assertThatThrownBy(() -> taskManagementService.claimTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

            verify(camundaService, times(0)).claimTask(any(), any());
        }

        @Test
        void claimTask_assigned_task_should_throw_exception() {
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            TaskResource taskResource = spy(TaskResource.class);

            when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);
            when(taskResource.getAssignee()).thenReturn("OTHER_USER_ID");
            when(taskResource.getTaskId()).thenReturn("TASK_ID");

            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            assertThatThrownBy(() -> taskManagementService.claimTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(ConflictException.class)
                .hasNoCause()
                .hasMessage("Task 'TASK_ID' is already claimed by someone else.");

            verify(camundaService, times(0)).claimTask(any(), any());

        }

        @Test
        void claimTask_assigned_task_should_succeed_for_same_user() {
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            TaskResource taskResource = spy(TaskResource.class);

            when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);
            when(taskResource.getAssignee()).thenReturn(IDAM_USER_ID);

            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(Optional.of(taskResource));

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            taskManagementService.claimTask(taskId, accessControlResponse);

            verify(camundaService, times(1)).assignTask(taskId, IDAM_USER_ID, false);
        }
    }

    @Nested
    @DisplayName("unclaimTask()")
    class Release2EndpointsUnclaimTask {
        @Test
        void unclaimTask_should_succeed() {
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(Optional.of(taskResource));

            when(taskResource.getState()).thenReturn(CFTTaskState.UNASSIGNED);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            boolean taskHasUnassigned = taskResource
                .getState().getValue()
                .equals(CFTTaskState.UNASSIGNED.getValue());
            taskManagementService.unclaimTask(taskId, accessControlResponse);

            verify(camundaService, times(1)).unclaimTask(taskId, taskHasUnassigned);
        }

        @Test
        void unclaimTask_should_succeed_gp_flag_on() {
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(UNCLAIM, UNASSIGN);
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
            when(taskResource.getState()).thenReturn(CFTTaskState.UNASSIGNED);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            boolean taskHasUnassigned = taskResource
                .getState().getValue()
                .equals(CFTTaskState.UNASSIGNED.getValue());
            taskManagementService.unclaimTask(taskId, accessControlResponse);

            verify(camundaService, times(1)).unclaimTask(taskId, taskHasUnassigned);
        }

        @Test
        void unclaimTask_should_succeed_assignee_null_gp_flag_on() {
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(UNCLAIM, UNASSIGN);
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            when(taskResource.getAssignee()).thenReturn(null);
            when(taskResource.getState()).thenReturn(CFTTaskState.UNASSIGNED);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            boolean taskHasUnassigned = taskResource
                .getState().getValue()
                .equals(CFTTaskState.UNASSIGNED.getValue());
            taskManagementService.unclaimTask(taskId, accessControlResponse);

            verify(camundaService, times(1)).unclaimTask(taskId, taskHasUnassigned);
        }

        @Test
        void unclaimTask_succeed_when_task_assignee_differs_from_user_and_role_is_senior_tribunal_caseworker() {
            unclaimTask_should_succeed();
        }

    }

    @Nested
    @DisplayName("assignTask()")
    class Release2EndpointsAssignTask {
        @Test
        void assignTask_should_succeed() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findByIdOnly(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftQueryService.getTask(
                anyString(), anyList(), any(PermissionRequirements.class))
            ).thenReturn(Optional.of(taskResource));

            PermissionRequirements otherRequirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(OWN, EXECUTE);
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(
                taskId, assigneeAccessControlResponse.getRoleAssignments(), otherRequirements)
            ).thenReturn(Optional.of(taskResource));

            when(taskResource.getState()).thenReturn(CFTTaskState.UNASSIGNED);
            when(taskResource.getAssignee()).thenReturn(SECONDARY_IDAM_USER_ID);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);


            taskManagementService.assignTask(taskId, assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            );

            boolean isTaskAssigned = taskResource.getState().getValue().equals(CFTTaskState.ASSIGNED.getValue());
            assertEquals(SECONDARY_IDAM_USER_ID, taskResource.getAssignee());
            verify(camundaService, times(1)).assignTask(taskId, IDAM_USER_ID, isTaskAssigned);
        }

        @Test
        void assignTask_should_throw_role_assignment_verification_exception_when_assignee_has_access_returns_false() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);

            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();

            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            when(cftQueryService.getTask(
                anyString(), anyList(), any(PermissionRequirements.class))
            ).thenReturn(Optional.of(taskResource));

            PermissionRequirements otherRequirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(OWN, EXECUTE);
            when(cftQueryService.getTask(
                taskId, assigneeAccessControlResponse.getRoleAssignments(), otherRequirements)
            ).thenReturn(Optional.empty());
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));


            Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
                taskManagementService.assignTask(
                    taskId,
                    assignerAccessControlResponse,
                    Optional.of(assigneeAccessControlResponse)
                ));
            assertEquals("Role Assignment Verification: "
                         + "The user being assigned the Task has failed the Role Assignment checks performed.",
                exception.getMessage());

            verify(camundaService, times(0)).assignTask(any(), any(), anyBoolean());
        }


        @Test
        void assignTask_should_throw_role_assignment_verification_exception_when_assigner_has_access_returns_false() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            lenient().when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            when(cftQueryService.getTask(
                anyString(), anyList(), any(PermissionRequirements.class)
            )).thenReturn(Optional.empty());
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
                taskManagementService.assignTask(
                    taskId,
                    assignerAccessControlResponse,
                    Optional.of(assigneeAccessControlResponse)
                ));
            assertEquals("Role Assignment Verification: "
                         + "The user assigning the Task has failed the Role Assignment checks performed.",
                exception.getMessage());

            verify(camundaService, times(0)).assignTask(any(), any(), anyBoolean());
        }

        @Test
        void assignTask_should_throw_exception_when_missing_required_arguments() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(null).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);

            Exception exception = assertThrowsExactly(NullPointerException.class, () ->
                taskManagementService.assignTask(
                    taskId,
                    assignerAccessControlResponse,
                    Optional.of(assigneeAccessControlResponse)
                ));
            assertEquals("Assigner userId cannot be null",
                exception.getMessage());

            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build());
            when(assigneeAccessControlResponse.getUserInfo())
                .thenReturn(UserInfo.builder().uid(null).build());

            TaskResource taskResource = spy(TaskResource.class);

            when(cftQueryService.getTask(
                anyString(), anyList(), any(PermissionRequirements.class))
            ).thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            exception = assertThrowsExactly(NullPointerException.class, () ->
                taskManagementService.assignTask(
                    taskId,
                    assignerAccessControlResponse,
                    Optional.of(assigneeAccessControlResponse)
                ));
            assertEquals("Assignee userId cannot be null",
                exception.getMessage());
        }

        @Test
        void assignTask_should_throw_exception_when_missing_required_arguments_with_granular_permission() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);
            RoleAssignment roleAssignment = mock(RoleAssignment.class);
            when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
            List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
            when(assignerAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            when(assigneeAccessControlResponse.getUserInfo())
                .thenReturn(UserInfo.builder().uid(null).build());

            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder().buildSingleType(ASSIGN);
            when(cftQueryService.getTask(
                taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
            ).thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            Exception exception = assertThrowsExactly(NullPointerException.class, () ->
                taskManagementService.assignTask(
                    taskId,
                    assignerAccessControlResponse,
                    Optional.of(assigneeAccessControlResponse)
                ));
            assertEquals("Assignee userId cannot be null",
                exception.getMessage());

        }

        @Test
        void assignTask_should_throw_role_assignment_verification_exception_when_requester_does_not_have_permission() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder().buildSingleType(ASSIGN);
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(
                taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
            ).thenReturn(Optional.empty());

            when(cftTaskDatabaseService.findByIdOnly(taskId))
                .thenReturn(Optional.of(taskResource));

            Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
                taskManagementService.assignTask(
                    taskId,
                    assignerAccessControlResponse,
                    Optional.of(assigneeAccessControlResponse)
                ));
            assertEquals("Role Assignment Verification: "
                         + "The user assigning the Task has failed the Role Assignment checks performed.",
                exception.getMessage());

            verify(camundaService, times(0)).assignTask(any(), any(), anyBoolean());
        }

        @Test
        void task_is_unassigned_and_requester_tries_to_assign_to_someone_should_succeed() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder().buildSingleType(ASSIGN);
            when(cftQueryService.getTask(
                taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
            ).thenReturn(Optional.of(taskResource));

            PermissionRequirements otherRequirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(OWN, EXECUTE);
            when(cftQueryService.getTask(
                taskId, assigneeAccessControlResponse.getRoleAssignments(), otherRequirements)
            ).thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);


            taskManagementService.assignTask(taskId, assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            );

            verify(taskResource).setAssignee(IDAM_USER_ID);
            verify(taskResource).setState(CFTTaskState.ASSIGNED);
            verify(camundaService, times(1)).assignTask(taskId, IDAM_USER_ID, false);
        }

        @Test
        void task_is_unassigned_and_requester_tries_to_assign_to_themselves_should_succeed() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(CLAIM).build();
            when(cftQueryService.getTask(
                taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
            ).thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            PermissionRequirements otherRequirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(OWN, EXECUTE);
            when(cftQueryService.getTask(
                taskId, assigneeAccessControlResponse.getRoleAssignments(), otherRequirements)
            ).thenReturn(Optional.of(taskResource));

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);


            taskManagementService.assignTask(taskId, assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            );

            verify(taskResource).setAssignee(SECONDARY_IDAM_USER_ID);
            verify(taskResource).setState(CFTTaskState.ASSIGNED);
            verify(camundaService, times(1)).assignTask(taskId, SECONDARY_IDAM_USER_ID, false);
        }

        @Test
        void task_is_assigned_to_someone_and_requester_tries_to_assign_to_themselves_should_succeed() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_CLAIM)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, CLAIM), PermissionJoin.AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND).build();
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(
                taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
            ).thenReturn(Optional.of(taskResource));

            PermissionRequirements otherRequirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(OWN, EXECUTE);
            when(cftQueryService.getTask(
                taskId, assigneeAccessControlResponse.getRoleAssignments(), otherRequirements)
            ).thenReturn(Optional.of(taskResource));

            when(taskResource.getAssignee()).thenReturn(THIRD_IDAM_USER_ID);
            when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);

            when(cftTaskDatabaseService.findByIdOnly(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            taskManagementService.assignTask(taskId, assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            );

            verify(taskResource).setAssignee(SECONDARY_IDAM_USER_ID);
            verify(taskResource).setState(CFTTaskState.ASSIGNED);
            verify(camundaService, times(1)).assignTask(taskId, SECONDARY_IDAM_USER_ID, false);
        }

        @Test
        void task_is_assigned_to_someone_and_requester_tries_to_assign_to_someone_new_should_succeed() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND).build();
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(
                taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
            ).thenReturn(Optional.of(taskResource));

            PermissionRequirements otherRequirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(OWN, EXECUTE);
            when(cftQueryService.getTask(
                taskId, assigneeAccessControlResponse.getRoleAssignments(), otherRequirements)
            ).thenReturn(Optional.of(taskResource));

            when(taskResource.getAssignee()).thenReturn(THIRD_IDAM_USER_ID);
            when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);

            when(cftTaskDatabaseService.findByIdOnly(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);


            taskManagementService.assignTask(taskId, assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            );

            verify(taskResource).setAssignee(IDAM_USER_ID);
            verify(taskResource).setState(CFTTaskState.ASSIGNED);
            verify(camundaService, times(1)).assignTask(taskId, IDAM_USER_ID, false);
        }

        @Test
        void task_is_assigned_to_requester_tries_to_assign_to_someone_new_should_succeed() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNCLAIM_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNCLAIM, ASSIGN), PermissionJoin.AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), PermissionJoin.AND).build();
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftTaskDatabaseService.findByIdOnly(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftQueryService.getTask(
                taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
            ).thenReturn(Optional.of(taskResource));

            PermissionRequirements otherRequirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(OWN, EXECUTE);
            when(cftQueryService.getTask(
                taskId, assigneeAccessControlResponse.getRoleAssignments(), otherRequirements)
            ).thenReturn(Optional.of(taskResource));

            when(taskResource.getAssignee()).thenReturn(SECONDARY_IDAM_USER_ID);
            when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            taskManagementService.assignTask(taskId, assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            );

            verify(taskResource).setAssignee(IDAM_USER_ID);
            verify(taskResource).setState(CFTTaskState.ASSIGNED);
            verify(camundaService, times(1)).assignTask(taskId, IDAM_USER_ID, false);
        }

        @Test
        void unAssignTask_should_succeed_with_granular_permission() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(UNASSIGN, UNCLAIM);
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(
                taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
            ).thenReturn(Optional.of(taskResource));

            when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);
            when(taskResource.getAssignee()).thenReturn(SECONDARY_IDAM_USER_ID);

            when(cftTaskDatabaseService.findByIdOnly(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            taskManagementService.assignTask(taskId, assignerAccessControlResponse, Optional.empty());

            verify(taskResource).setAssignee(null);
            verify(camundaService, times(1)).unclaimTask(taskId, false);
        }

        @Test
        void task_is_assigned_to_requester_tries_to_assign_to_themselves_should_succeed() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);
            when(taskResource.getAssignee()).thenReturn(SECONDARY_IDAM_USER_ID);
            when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);

            when(cftTaskDatabaseService.findByIdOnly(taskId))
                .thenReturn(Optional.of(taskResource));

            taskManagementService.assignTask(taskId, assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            );

            verify(taskResource, never()).setAssignee(any());
            verify(taskResource, never()).setState(any());
            verify(cftTaskDatabaseService, never()).findByIdAndObtainPessimisticWriteLock(taskId);
            verify(cftTaskDatabaseService, never()).saveTask(taskResource);
            verify(camundaService, never()).assignTask(eq(taskId), anyString(), eq(false));
        }

        @Test
        void task_is_assigned_to_someone_and_tries_to_assign_to_same_assignee_should_succeed() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assigneeAccessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);
            when(taskResource.getAssignee()).thenReturn(IDAM_USER_ID);
            when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);

            when(cftTaskDatabaseService.findByIdOnly(taskId))
                .thenReturn(Optional.of(taskResource));

            taskManagementService.assignTask(taskId, assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            );

            verify(taskResource, never()).setAssignee(any());
            verify(taskResource, never()).setState(any());
            verify(cftTaskDatabaseService, never()).findByIdAndObtainPessimisticWriteLock(taskId);
            verify(cftTaskDatabaseService, never()).saveTask(taskResource);
            verify(camundaService, never()).assignTask(eq(taskId), anyString(), eq(false));
        }

        @Test
        void task_is_unassigned_and_tries_to_unassign_should_succeed() {
            AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
            userInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(assignerAccessControlResponse.getUserInfo())
                .thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findByIdOnly(taskId))
                .thenReturn(Optional.of(taskResource));

            taskManagementService.assignTask(taskId, assignerAccessControlResponse, Optional.empty());

            verify(taskResource, never()).setAssignee(any());
            verify(taskResource, never()).setState(any());
            verify(cftTaskDatabaseService, never()).findByIdAndObtainPessimisticWriteLock(taskId);
            verify(cftTaskDatabaseService, never()).saveTask(taskResource);
            verify(camundaService, never()).assignTask(eq(taskId), anyString(), eq(false));
        }

        @ParameterizedTest
        @CsvSource({
            "newAssignee, , Assigner, Assign",
            "assigner, , assigner, Claim",
            ", oldAssignee, assigner, Unassign",
            ", assigner, assigner, Unclaim",
            "newAssignee, oldAssignee, assigner, UnassignAssign",
            "assigner, oldAssignee, assigner, UnassignClaim",
            "newAssignee, assigner, assigner, UnclaimAssign",
            "newAssignee, newAssignee, assigner, ",
            ", , Assigner, ",
        })
        void should_build_task_action_correctly_when_task_is_assigned(String newAssignee, String oldAssignee,
                                                                      String assigner, String taskAction) {
            TaskResource taskResource = spy(TaskResource.class);

            taskManagementService.updateTaskActionAttributesForAssign(
                taskResource,
                assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)
            );

            if (taskAction != null) {
                assertEquals(TaskAction.from(taskAction).get().getValue(), taskResource.getLastUpdatedAction());
            } else {
                assertNull(taskResource.getLastUpdatedAction());
            }
        }
    }

    @Nested
    @DisplayName("cancelTask()")
    class Release2EndpointsCancelTask {
        @Test
        void cancelTask_should_succeed() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(Optional.of(taskResource));

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));


            TaskRoleResource taskRoleResource = new TaskRoleResource(
                "tribunal-caseworker",
                true,
                false,
                false,
                false,
                true,
                false,
                new String[]{"SPECIFIC", "STANDARD"},
                0,
                false,
                "JUDICIAL",
                taskId,
                OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false
            );
            Set<TaskRoleResource> taskRoleResourcesItems = new HashSet<>(List.of(taskRoleResource));

            when(taskResource.getTaskRoleResources()).thenReturn(taskRoleResourcesItems);

            taskManagementService.cancelTask(taskId, accessControlResponse);

            assertEquals(CFTTaskState.CANCELLED, taskResource.getState());
            verify(camundaService, times(1)).cancelTask(taskId);
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        }

        @Test
        void cancelTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(Optional.empty());
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            assertThatThrownBy(() -> taskManagementService.cancelTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

            verify(camundaService, times(0)).cancelTask(any());
        }

        @Test
        void cancelTask_should_throw_role_assignment_verification_exception_when_granular_permission_access_fail() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(CANCEL, CANCEL_OWN);
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.empty());
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            assertThatThrownBy(() -> taskManagementService.cancelTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

            verify(camundaService, times(0)).cancelTask(any());
        }

        @Test
        void cancelTask_should_throw_verification_exception_when_granular_permission_access_fail_for_null_assignee() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);
            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(CANCEL, CANCEL_OWN);
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(taskResource.getAssignee()).thenReturn(null);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                "roleName", false, false, false, false, false,
                false, new String[]{}, 1, false, "roleCategory"
            );
            Set<TaskRoleResource> taskRoleResourcesItems = new HashSet<>();
            taskRoleResourcesItems.add(taskRoleResource);
            when(taskResource.getTaskRoleResources()).thenReturn(taskRoleResourcesItems);

            assertThatThrownBy(() -> taskManagementService.cancelTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

            verify(camundaService, times(0)).cancelTask(any());
        }

        @Test
        void cancelTask_should_throw_exception_when_missing_required_arguments() {

            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());
            assertThatThrownBy(() -> taskManagementService.cancelTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(NullPointerException.class)
                .hasNoCause()
                .hasMessage("UserId cannot be null");
        }

        @Test
        void should_throw_exception_when_task_not_found() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            TaskResource taskResource = spy(TaskResource.class);
            PermissionRequirementBuilder.builder().buildSingleType(CANCEL);
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskManagementService.cancelTask(taskId, accessControlResponse))
                .isInstanceOf(TaskNotFoundException.class)
                .hasNoCause()
                .hasMessage("Task Not Found Error: The task could not be found.");
            verify(camundaService, times(0)).cancelTask(any());
            verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
        }

        @Test
        void should_update_task_state_terminated_when_cft_task_state_is_null_and_feign_exception_thrown() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            when(camundaService.isCftTaskStateExistInCamunda(taskId)).thenReturn(false);
            when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);

            doThrow(TaskCancelException.class)
                .when(camundaService).cancelTask(taskId);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                "tribunal-caseworker",
                true,
                false,
                false,
                false,
                true,
                false,
                new String[]{"SPECIFIC", "STANDARD"},
                0,
                false,
                "JUDICIAL",
                taskId,
                OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false
            );
            Set<TaskRoleResource> taskRoleResourcesItems = new HashSet<>(List.of(taskRoleResource));

            when(taskResource.getTaskRoleResources()).thenReturn(taskRoleResourcesItems);

            taskManagementService.cancelTask(taskId, accessControlResponse);

            verify(camundaServiceApi, never()).bpmnEscalation(any(), anyString(), anyMap());
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);

            verify(cftTaskDatabaseService).saveTask(taskResourceCaptor.capture());

            TaskResource expectedTaskResource = taskResource;
            expectedTaskResource.setState(TERMINATED);

            TaskResource savedTaskResource = taskResourceCaptor.getValue();

            assertEquals(expectedTaskResource, savedTaskResource);
        }

        @Test
        void should_not_update_task_state_when_cft_feign_exception_thrown_and_task_already_terminated() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(Optional.of(taskResource));

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));

            when(camundaService.isCftTaskStateExistInCamunda(taskId)).thenReturn(false);
            when(taskResource.getState()).thenReturn(TERMINATED);

            doThrow(TaskCancelException.class)
                .when(camundaService).cancelTask(taskId);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                "tribunal-caseworker",
                true,
                false,
                false,
                false,
                true,
                false,
                new String[]{"SPECIFIC", "STANDARD"},
                0,
                false,
                "JUDICIAL",
                taskId,
                OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false
            );
            Set<TaskRoleResource> taskRoleResourcesItems = new HashSet<>(List.of(taskRoleResource));

            when(taskResource.getTaskRoleResources()).thenReturn(taskRoleResourcesItems);
            assertThatThrownBy(() -> taskManagementService.cancelTask(taskId, accessControlResponse))
                .isInstanceOf(TaskCancelException.class);

            verify(camundaServiceApi, never()).bpmnEscalation(any(), anyString(), anyMap());

            verify(cftTaskDatabaseService, never()).saveTask(any(TaskResource.class));
        }

        @Test
        void cancelTask_should_succeed_and_granular_feature_flag_is_on_with_cancel() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);
            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(asList(CANCEL, CANCEL_OWN), PermissionJoin.OR).build();
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
            when(taskResource.getAssignee()).thenReturn(IDAM_USER_ID);
            TaskRoleResource taskRoleResource = new TaskRoleResource(
                "roleName", false, false, false, false, false,
                false, new String[]{}, 1, false, "roleCategory"
            );
            Set<TaskRoleResource> taskRoleResourcesItems = new HashSet<>();
            taskRoleResourcesItems.add(taskRoleResource);
            when(taskResource.getTaskRoleResources()).thenReturn(taskRoleResourcesItems);

            taskManagementService.cancelTask(taskId, accessControlResponse);

            assertEquals(CFTTaskState.CANCELLED, taskResource.getState());
            verify(camundaService, times(1)).cancelTask(taskId);
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);

        }

        @Test
        void cancelTask_should_succeed_and_granular_feature_flag_is_on_with_cancel_own() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);
            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(asList(CANCEL, CANCEL_OWN), PermissionJoin.OR).build();
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
            when(taskResource.getAssignee()).thenReturn(IDAM_USER_ID);
            TaskRoleResource taskRoleResource = new TaskRoleResource(
                "roleName", false, false, false, false, false,
                false, new String[]{}, 1, false, "roleCategory"
            );
            Set<TaskRoleResource> taskRoleResourcesItems = new HashSet<>();
            taskRoleResourcesItems.add(taskRoleResource);
            when(taskResource.getTaskRoleResources()).thenReturn(taskRoleResourcesItems);

            taskManagementService.cancelTask(taskId, accessControlResponse);

            assertEquals(CFTTaskState.CANCELLED, taskResource.getState());
            verify(camundaService, times(1)).cancelTask(taskId);
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);

        }
    }

    @Nested
    @DisplayName("completeTask()")
    class Release2EndpointsCompleteTask {
        @Test
        void completeTask_should_succeed_gp_flag_on() {
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            TaskResource taskResource = spy(TaskResource.class);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(asList(OWN, EXECUTE), OR)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(COMPLETE), OR)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(COMPLETE_OWN), OR)
                .build();
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.of(taskResource));
            when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);
            when(taskResource.getAssignee()).thenReturn(IDAM_USER_ID);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));

            taskManagementService.completeTask(taskId, accessControlResponse, requestParamMap);
            boolean taskStateIsCompletedAlready = taskResource.getState().equals(CFTTaskState.COMPLETED);
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
            verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
        }

        @Test
        void completeTask_should_throw_role_assignment_verification_exception_when_role_is_incorrect_gp_flag_on() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(asList(OWN, EXECUTE), OR)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(COMPLETE), OR)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(COMPLETE_OWN), OR)
                .build();
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.empty());
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

            assertThatThrownBy(() -> taskManagementService.completeTask(
                taskId,
                accessControlResponse,
                requestParamMap
            ))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

            verify(camundaService, times(0)).completeTask(any(), anyBoolean());
        }

        @Test
        void completeTask_should_throw_task_state_incorrect_exception_when_task_has_no_assignee() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);
            PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(asList(OWN, EXECUTE), OR)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(COMPLETE), OR)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(COMPLETE_OWN), OR)
                .build();
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
            when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                .thenReturn(Optional.of(taskResource));
            when(taskResource.getAssignee()).thenReturn(null);

            assertThatThrownBy(() -> taskManagementService.completeTask(
                taskId,
                accessControlResponse,
                requestParamMap
            ))
                .isInstanceOf(TaskStateIncorrectException.class)
                .hasNoCause()
                .hasMessage(
                    String.format("Could not complete task with id: %s as task was not previously assigned", taskId)
                );

            verify(camundaService, times(0)).completeTask(any(), anyBoolean());
        }

        @Test
        void completeTask_should_throw_exception_when_missing_required_arguments() {

            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());

            assertThatThrownBy(() -> taskManagementService.completeTask(
                taskId,
                accessControlResponse,
                requestParamMap
            ))
                .isInstanceOf(NullPointerException.class)
                .hasNoCause()
                .hasMessage("UserId cannot be null");
        }

        @Test
        void should_throw_task_not_exception_when_task_resource_not_found() {

            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = spy(TaskResource.class);
            when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskManagementService.completeTask(taskId, accessControlResponse, requestParamMap))
                .isInstanceOf(TaskNotFoundException.class)
                .hasNoCause()
                .hasMessage("Task Not Found Error: The task could not be found.");
            verify(camundaService, times(0)).completeTask(any(), anyBoolean());
            verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
        }
    }

    @Nested
    @DisplayName("completeTaskWithPrivilegeAndCompletionOptions()")
    class CompleteTaskWithPrivilegeAndCompletionOptions {

        @Test
        void should_throw_exception_when_missing_required_arguments() {

            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());

            assertThatThrownBy(() -> taskManagementService.cancelTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(NullPointerException.class)
                .hasNoCause()
                .hasMessage("UserId cannot be null");
        }

        @Nested
        @DisplayName("when assignAndComplete completion option is true")
        class AssignAndCompleteIsTrue {

            @Test
            void should_succeed_AssignAndComplete() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));
                PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                    .buildSingleRequirementWithOr(OWN, EXECUTE);
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
                when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                    .thenReturn(Optional.of(taskResource));
                when(taskResource.getState())
                    .thenReturn(CFTTaskState.COMPLETED);
                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(true),
                    requestParamMap
                );
                boolean taskStateIsAssignedAlready = TaskState.ASSIGNED.value()
                    .equals(mockedVariables.get("taskState"));
                assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
                verify(camundaService, times(1))
                    .assignAndCompleteTask(taskId, IDAM_USER_ID, taskStateIsAssignedAlready);
            }

            @Test
            void should_throw_role_assignment_verification_exception_when_has_access_is_false_and_completion_options() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
                List<RoleAssignment> roleAssignments = List.of(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);

                PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                    .buildSingleRequirementWithOr(OWN, EXECUTE);

                when(cftQueryService.getTask(taskId, roleAssignments, requirements))
                    .thenReturn(Optional.empty());
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

                Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(true),
                        requestParamMap
                    ));
                assertEquals("Role Assignment Verification: "
                             + "The request failed the Role Assignment checks performed.",
                    exception.getMessage());

            }

            @Test
            void should_throw_exception_when_task_resource_not_found() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                TaskResource taskResource = spy(TaskResource.class);

                Exception exception = assertThrowsExactly(TaskNotFoundException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(true),
                        requestParamMap
                    ));
                assertEquals("Task Not Found Error: The task could not be found.",
                    exception.getMessage());

                verify(camundaService, times(0)).assignAndCompleteTask(any(), any(), anyBoolean());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }

        }

        @Nested
        @DisplayName("when assignAndComplete completion option is false")
        class AssignAndCompleteIsFalse {

            @Test
            void should_succeed_AssignAndCompleteIsFalse_gp_flag_on() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);


                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));
                PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                    .initPermissionRequirement(asList(OWN, EXECUTE), OR)
                    .joinPermissionRequirement(OR)
                    .nextPermissionRequirement(List.of(COMPLETE), OR)
                    .joinPermissionRequirement(OR)
                    .nextPermissionRequirement(List.of(COMPLETE_OWN), OR)
                    .build();
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
                when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                    .thenReturn(Optional.of(taskResource));
                when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
                when(taskResource.getState()).thenReturn(ASSIGNED);
                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false),
                    requestParamMap
                );
                boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value()
                    .equals(mockedVariables.get("taskState"));
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
                verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
            }

            @Test
            void should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
                List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

                Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false),
                        requestParamMap
                    ));
                assertEquals("Role Assignment Verification: "
                             + "The request failed the Role Assignment checks performed.",
                    exception.getMessage());

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
            }

            @Test
            void should_throw_task_state_incorrect_exception_when_task_has_no_assignee() {

                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                TaskResource taskResource = spy(TaskResource.class);
                PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                    .initPermissionRequirement(asList(OWN, EXECUTE), OR)
                    .joinPermissionRequirement(OR)
                    .nextPermissionRequirement(List.of(COMPLETE), OR)
                    .joinPermissionRequirement(OR)
                    .nextPermissionRequirement(List.of(COMPLETE_OWN), OR)
                    .build();
                when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                    .thenReturn(Optional.of(taskResource));
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

                Exception exception = assertThrowsExactly(TaskStateIncorrectException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false),
                        requestParamMap
                    ));
                assertEquals(
                    String.format("Could not complete task with id: %s as task was not previously assigned", taskId),
                    exception.getMessage());

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
            }

            @Test
            void should_throw_exception_when_task_resource_not_found() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
                TaskResource taskResource = spy(TaskResource.class);

                Exception exception = assertThrowsExactly(TaskNotFoundException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false),
                        requestParamMap
                    ));
                assertEquals("Task Not Found Error: The task could not be found.",
                    exception.getMessage());

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }
        }
    }

    @Nested
    @DisplayName("completeTaskWithPrivilegeAndCompletionOptions()")
    class Release2EndpointsCompleteTaskWithPrivilegeAndCompletionOptions {

        @Test
        void should_throw_exception_when_missing_required_arguments() {

            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());

            assertThatThrownBy(() -> taskManagementService.cancelTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(NullPointerException.class)
                .hasNoCause()
                .hasMessage("UserId cannot be null");
        }

        @Nested
        @DisplayName("when assignAndComplete completion option is true")
        class AssignAndCompleteIsTrue {

            @Test
            void should_succeed_AssignAndCompleteIsTrue() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                TaskResource taskResource = spy(TaskResource.class);
                when(taskResource.getState()).thenReturn(CFTTaskState.COMPLETED);
                PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                    .buildSingleRequirementWithOr(OWN, EXECUTE);
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
                when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                    .thenReturn(Optional.of(taskResource));

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));
                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(true),
                    requestParamMap
                );
                boolean taskStateIsAssignededAlready = TaskState.ASSIGNED.value()
                    .equals(taskResource.getState().getValue());
                assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
                verify(camundaService, times(1))
                    .assignAndCompleteTask(taskId, IDAM_USER_ID, taskStateIsAssignededAlready);
            }

            @Test
            void should_throw_role_assignment_verification_exception_when_has_access_is_false_and_completion_options() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                    .buildSingleRequirementWithOr(OWN, EXECUTE);
                when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                    .thenReturn(Optional.empty());
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

                Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(true),
                        requestParamMap
                    ));
                assertEquals("Role Assignment Verification: "
                             + "The request failed the Role Assignment checks performed.",
                    exception.getMessage());

            }

            @Test
            void should_throw_task_not_found_exception_when_task_resource_not_found() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                PermissionRequirementBuilder.builder()
                    .buildSingleRequirementWithOr(OWN, EXECUTE);

                Exception exception = assertThrowsExactly(TaskNotFoundException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(true),
                        requestParamMap
                    ));
                assertEquals("Task Not Found Error: The task could not be found.",
                    exception.getMessage());
                TaskResource taskResource = spy(TaskResource.class);
                verify(camundaService, times(0)).assignAndCompleteTask(any(), any(), anyBoolean());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }

        }

        @Nested
        @DisplayName("when assignAndComplete completion option is false")
        class AssignAndCompleteIsFalse {

            @Test
            void should_succeed_AssignAndCompleteIsFalse_gp_flag_on() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                TaskResource taskResource = spy(TaskResource.class);
                when(taskResource.getState()).thenReturn(CFTTaskState.COMPLETED);
                PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                    .initPermissionRequirement(asList(OWN, EXECUTE), OR)
                    .joinPermissionRequirement(OR)
                    .nextPermissionRequirement(List.of(COMPLETE), OR)
                    .joinPermissionRequirement(OR)
                    .nextPermissionRequirement(List.of(COMPLETE_OWN), OR)
                    .build();
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
                when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                    .thenReturn(Optional.of(taskResource));
                when(taskResource.getAssignee()).thenReturn(IDAM_USER_ID);
                when(taskResource.getState()).thenReturn(CFTTaskState.CANCELLED);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false),
                    requestParamMap
                );
                boolean taskStateIsCompletedAlready = CFTTaskState.COMPLETED.getValue()
                    .equals(taskResource.getState().getValue());
                assertEquals(CFTTaskState.CANCELLED, taskResource.getState());
                verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
            }

            @Test
            void should_succeed_task_already_complete() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
                List<RoleAssignment> roleAssignments = List.of(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
                TaskResource taskResource = spy(TaskResource.class);
                when(taskResource.getState()).thenReturn(CFTTaskState.COMPLETED);
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

                when(cftQueryService.getTask(any(), anyList(),
                    any(PermissionRequirements.class)
                )).thenReturn(Optional.of(taskResource));

                when(taskResource.getAssignee()).thenReturn(IDAM_USER_ID);
                when(taskResource.getState()).thenReturn(CFTTaskState.COMPLETED);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false),
                    requestParamMap
                );
                boolean taskStateIsCompletedAlready = CFTTaskState.COMPLETED.getValue()
                    .equals(taskResource.getState().getValue());
                assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
                verify(camundaService, times(0)).completeTask(taskId, taskStateIsCompletedAlready);
            }

            @Test
            void should_succeed_task_already_terminated() {
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
                List<RoleAssignment> roleAssignments = List.of(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
                TaskResource taskResource = spy(TaskResource.class);
                when(taskResource.getState()).thenReturn(CFTTaskState.TERMINATED);
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

                when(cftQueryService.getTask(any(), anyList(),
                    any(PermissionRequirements.class)
                )).thenReturn(Optional.of(taskResource));

                when(taskResource.getAssignee()).thenReturn(IDAM_USER_ID);
                when(taskResource.getState()).thenReturn(CFTTaskState.TERMINATED);
                when(taskResource.getTerminationReason()).thenReturn("completed");

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false),
                    requestParamMap
                );
                boolean taskStateIsCompletedAlready = CFTTaskState.TERMINATED.getValue()
                    .equals(taskResource.getState().getValue());
                assertEquals(CFTTaskState.TERMINATED, taskResource.getState());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
                verify(camundaService, times(0)).completeTask(taskId, taskStateIsCompletedAlready);
            }


            @Test
            void should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
                List<RoleAssignment> roleAssignments = List.of(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);

                lenient().when(cftQueryService.getTask(any(), anyList(), anyList()))
                    .thenReturn(Optional.empty());
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));

                Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false),
                        requestParamMap
                    ));
                assertEquals("Role Assignment Verification: "
                             + "The request failed the Role Assignment checks performed.",
                    exception.getMessage());

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
            }

            @Test
            void should_throw_task_state_incorrect_exception_when_task_has_no_assignee() {

                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
                List<RoleAssignment> roleAssignments = List.of(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of(caseId));
                when(cftQueryService.getTask(any(), anyList(), any(PermissionRequirements.class)))
                    .thenReturn(Optional.of(taskResource));
                when(taskResource.getAssignee()).thenReturn(null);

                Exception exception = assertThrowsExactly(TaskStateIncorrectException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false),
                        requestParamMap
                    ));
                assertEquals(
                    String.format("Could not complete task with id: %s as task was not previously assigned", taskId),
                    exception.getMessage()
                );

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
            }


            @Test
            void should_throw_task_not_found_exception_when_task_resource_not_found() {
                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                List<RoleAssignment> roleAssignments = List.of(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
                userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                PermissionRequirementBuilder.builder()
                    .buildSingleRequirementWithOr(OWN, EXECUTE);

                TaskResource taskResource = spy(TaskResource.class);

                Exception exception = assertThrowsExactly(TaskNotFoundException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false),
                        requestParamMap
                    ));
                assertEquals(
                    "Task Not Found Error: The task could not be found.",
                    exception.getMessage()
                );

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }
        }
    }

    @Nested
    @DisplayName("newInitiateTask()")
    class NewInitiateTask {
        OffsetDateTime dueDate = OffsetDateTime.now();
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        private InitiateTaskRequestMap initiateTaskRequest = new InitiateTaskRequestMap(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), A_TASK_TYPE,
                TASK_NAME.value(), A_TASK_NAME,
                DUE_DATE.value(), formattedDueDate,
                ROLE_ASSIGNMENT_ID.value(), SOME_ROLE_ASSIGNMENT_ID
            )
        );
        @Mock
        private TaskResource taskResource;

        @Test
        void given_some_error_other_than_DataAccessException_when_requiring_lock_then_throw_500_error()
            throws SQLException {
            doThrow(new RuntimeException("some unexpected error"))
                .when(cftTaskDatabaseService).insertAndLock(anyString(), any());

            assertThatThrownBy(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest))
                .isInstanceOf(RuntimeException.class);
        }

        @Test
        void should_throw_exception_for_missing_mandatory_fields_on_task_initiation()
            throws ServiceMandatoryFieldValidationException {
            lenient().when(configureTaskService.configureCFTTask(any(TaskResource.class), any(TaskToConfigure.class)))
                .thenReturn(taskResource);
            lenient().doThrow(new ServiceMandatoryFieldValidationException("some unexpected error"))
                .when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));
            assertThatThrownBy(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest))
                .isInstanceOf(ServiceMandatoryFieldValidationException.class);
        }

        @Test
        void given_some_error_when_initiateTaskProcess_then_throw_500_error() {
            when(cftTaskMapper.readDate(any(), any(CamundaVariableDefinition.class), any())).thenCallRealMethod();
            doThrow(new RuntimeException("some unexpected error"))
                .when(cftTaskMapper).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());

            assertThatThrownBy(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest))
                .isInstanceOf(GenericServerErrorException.class)
                .hasMessage("Generic Server Error: The action could not be completed "
                            + "because there was a problem when initiating the task.");
        }

        @Test
        void given_DataAccessException_when_initiate_task_then_throw_503_error() throws SQLException {
            String msg = "duplicate key value violates unique constraint \"tasks_pkey\"";
            doThrow(new DataIntegrityViolationException(msg))
                .when(cftTaskDatabaseService).insertAndLock(anyString(), any());

            assertThatThrownBy(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest))
                .isInstanceOf(DatabaseConflictException.class)
                .hasMessage("Database Conflict Error: "
                            + "The action could not be completed because there was a conflict in the database.");
        }

        @Test
        void given_initiateTask_task_is_initiated() {

            initiateTaskRequest = new InitiateTaskRequestMap(
                INITIATION,
                Map.of(
                    TASK_TYPE.value(), A_TASK_TYPE,
                    TASK_NAME.value(), A_TASK_NAME,
                    DUE_DATE.value(), formattedDueDate,
                    ROLE_ASSIGNMENT_ID.value(), SOME_ROLE_ASSIGNMENT_ID,
                    "taskId", taskId
                )
            );
            mockInitiateTaskDependencies(CFTTaskState.UNASSIGNED);
            taskManagementService.initiateTask(taskId, initiateTaskRequest);

            verifyExpectations(CFTTaskState.UNASSIGNED);
        }

        private void verifyExpectations(CFTTaskState cftTaskState) {
            verify(cftTaskMapper, atLeastOnce()).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());

            Map<String, Object> taskAttributes = new HashMap<>(initiateTaskRequest.getTaskAttributes());
            OffsetDateTime offsetDueDate = OffsetDateTime.parse(formattedDueDate, CAMUNDA_DATA_TIME_FORMATTER);
            taskAttributes.put(DUE_DATE.value(), offsetDueDate);

            verify(configureTaskService).configureCFTTask(
                eq(taskResource),
                argThat(taskToConfigure -> taskToConfigure.equals(new TaskToConfigure(
                    taskId,
                    A_TASK_TYPE,
                    "aCaseId",
                    A_TASK_NAME,
                    taskAttributes
                )))
            );

            verify(taskAutoAssignmentService).performAutoAssignment(taskId, taskResource);

            if (cftTaskState.equals(CFTTaskState.ASSIGNED) || cftTaskState.equals(CFTTaskState.UNASSIGNED)) {
                verify(camundaService).updateCftTaskState(
                    taskId,
                    cftTaskState.equals(CFTTaskState.ASSIGNED) ? TaskState.ASSIGNED : TaskState.UNASSIGNED
                );
            } else {
                verifyNoInteractions(camundaService);
            }

            verify(cftTaskDatabaseService).saveTask(taskResource);
        }

        private void mockInitiateTaskDependencies(CFTTaskState cftTaskState) {
            when(cftTaskMapper.readDate(any(), any(CamundaVariableDefinition.class), any())).thenCallRealMethod();
            when(cftTaskMapper.mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes()))
                .thenReturn(taskResource);
            OffsetDateTime offsetDueDate = OffsetDateTime.parse(formattedDueDate, CAMUNDA_DATA_TIME_FORMATTER);

            when(taskResource.getTaskType()).thenReturn(A_TASK_TYPE);
            when(taskResource.getTaskId()).thenReturn(taskId);
            when(taskResource.getCaseId()).thenReturn("aCaseId");
            when(taskResource.getTaskName()).thenReturn(A_TASK_NAME);
            when(taskResource.getState()).thenReturn(cftTaskState);
            when(taskResource.getDueDateTime()).thenReturn(offsetDueDate);

            lenient().when(configureTaskService.configureCFTTask(any(TaskResource.class), any(TaskToConfigure.class)))
                .thenReturn(taskResource);
            when(taskAutoAssignmentService.performAutoAssignment(any(), any(TaskResource.class)))
                .thenReturn(taskResource);
            when(cftTaskDatabaseService.saveTask(any(TaskResource.class))).thenReturn(taskResource);
        }

    }

    @Nested
    @DisplayName("terminateTask()")
    class TerminateTask {

        @Nested
        @DisplayName("When Terminate Reason is Completed")
        class Completed {

            TerminateInfo terminateInfo = new TerminateInfo("completed");

            @Test
            void should_succeed() {

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));

                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                taskManagementService.terminateTask(taskId, terminateInfo);

                assertEquals(TERMINATED, taskResource.getState());
                assertEquals("completed", taskResource.getTerminationReason());
                verify(camundaService, times(1)).deleteCftTaskState(taskId);
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
            }

            @Test
            void should_handle_when_task_resource_not_found_and_delete_task_in_camunda() {
                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.empty());

                taskManagementService.terminateTask(taskId, terminateInfo);

                verify(camundaService, times(1)).deleteCftTaskState(taskId);
                verify(cftTaskDatabaseService, times(0)).saveTask(any(TaskResource.class));
            }

        }

        @Nested
        @DisplayName("When Terminate Reason is Cancelled")
        class Cancelled {
            TerminateInfo terminateInfo = new TerminateInfo("cancelled");


            @Test
            void should_succeed() {

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));

                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                taskManagementService.terminateTask(taskId, terminateInfo);

                assertEquals(TERMINATED, taskResource.getState());
                assertEquals("cancelled", taskResource.getTerminationReason());
                verify(camundaService, times(1)).deleteCftTaskState(taskId);
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
            }


            @Test
            void should_handle_when_task_resource_not_found_and_delete_task_in_camunda() {
                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.empty());

                taskManagementService.terminateTask(taskId, terminateInfo);

                verify(camundaService, times(1)).deleteCftTaskState(taskId);
                verify(cftTaskDatabaseService, times(0)).saveTask(any(TaskResource.class));
            }

        }

        @Nested
        @DisplayName("When Terminate Reason is Deleted")
        class Deleted {
            TerminateInfo terminateInfo = new TerminateInfo("deleted");

            @Test
            void should_succeed() {

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));

                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                taskManagementService.terminateTask(taskId, terminateInfo);

                assertEquals(TERMINATED, taskResource.getState());
                assertEquals("deleted", taskResource.getTerminationReason());
                verify(camundaService, times(1)).deleteCftTaskState(taskId);
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
            }

            @Test
            void should_handle_when_task_resource_not_found_and_delete_task_in_camunda() {
                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.empty());

                taskManagementService.terminateTask(taskId, terminateInfo);

                verify(camundaService, times(1)).deleteCftTaskState(taskId);
                verify(cftTaskDatabaseService, times(0)).saveTask(any(TaskResource.class));
            }

        }

    }

    @Nested
    @DisplayName("updateNotes()")
    class UpdateNotes {
        private static final String MUST_NOT_BE_EMPTY = "must not be empty";

        @Test
        void should_succeed() {
            List<NoteResource> existingNotesList = new ArrayList<>();
            final NoteResource existingNoteResource = new NoteResource(
                "someCode",
                "noteTypeVal",
                "userVal",
                "someContent"
            );
            existingNotesList.add(existingNoteResource);

            TaskResource taskResourceById = new TaskResource(
                "taskId", "taskName", "taskType", CFTTaskState.ASSIGNED
            );
            taskResourceById.setNotes(existingNotesList);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(any()))
                .thenReturn(Optional.of(taskResourceById));

            List<NoteResource> mergedNotesList = new ArrayList<>();
            NoteResource noteResource = new NoteResource(
                "Warning Code",
                "Warning",
                "userId",
                "Warning Description"
            );
            mergedNotesList.add(noteResource);
            noteResource = new NoteResource(
                "Warning Code",
                "Warning",
                "userId",
                "Warning Description"
            );
            mergedNotesList.add(noteResource);

            TaskResource mergedTaskResource = new TaskResource(
                "taskId", "taskName", "taskType", CFTTaskState.ASSIGNED
            );

            mergedTaskResource.setNotes(mergedNotesList);
            setTaskActionAttributes(mergedTaskResource, IDAM_SYSTEM_USER, TaskAction.ADD_WARNING);
            when(cftTaskDatabaseService.saveTask(any()))
                .thenReturn(mergedTaskResource);

            final NoteResource newNoteResource = new NoteResource(
                "Warning Code",
                "Warning",
                "userId",
                "Warning Description"
            );
            List<NoteResource> newNotes = new ArrayList<>();
            newNotes.add(newNoteResource);
            NotesRequest notesRequest = new NotesRequest(newNotes);

            final TaskResource expected = taskManagementService.updateNotes("taskId", notesRequest);

            assertEquals(expected, mergedTaskResource);
            verify(cftTaskDatabaseService, times(1))
                .findByIdAndObtainPessimisticWriteLock(any());
            verify(cftTaskDatabaseService, times(1))
                .saveTask(any());
        }

        @Test
        void should_succeed_when_task_has_no_existing_notes() {
            TaskResource taskResourceById = new TaskResource(
                "taskId", "taskName", "taskType", CFTTaskState.ASSIGNED
            );
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(any()))
                .thenReturn(Optional.of(taskResourceById));

            List<NoteResource> mergedNotesList = new ArrayList<>();
            NoteResource noteResource = new NoteResource(
                "Warning Code",
                "Warning",
                "userId",
                "Warning Description"
            );
            mergedNotesList.add(noteResource);

            TaskResource mergedTaskResource = new TaskResource(
                "taskId", "taskName", "taskType", CFTTaskState.ASSIGNED
            );
            setTaskActionAttributes(mergedTaskResource, IDAM_SYSTEM_USER, TaskAction.ADD_WARNING);
            mergedTaskResource.setNotes(mergedNotesList);
            when(cftTaskDatabaseService.saveTask(any()))
                .thenReturn(mergedTaskResource);

            final NoteResource newNoteResource = new NoteResource(
                "Warning Code",
                "Warning",
                "userId",
                "Warning Description"
            );
            List<NoteResource> newNotes = new ArrayList<>();
            newNotes.add(newNoteResource);
            NotesRequest notesRequest = new NotesRequest(newNotes);

            final TaskResource expected = taskManagementService.updateNotes("taskId", notesRequest);

            assertEquals(expected, mergedTaskResource);
            verify(cftTaskDatabaseService, times(1))
                .findByIdAndObtainPessimisticWriteLock(any());
            verify(cftTaskDatabaseService, times(1))
                .saveTask(any());
        }

        @Test
        void should_throw_resource_not_found_exception_when_task_id_not_present() {
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock("taskId"))
                .thenReturn(Optional.empty());

            final NoteResource newNoteResource = new NoteResource(
                "Warning Code",
                "Warning",
                "userId",
                "Warning Description"
            );
            List<NoteResource> newNotes = new ArrayList<>();
            newNotes.add(newNoteResource);
            NotesRequest notesRequest = new NotesRequest(newNotes);

            assertThatThrownBy(() -> taskManagementService.updateNotes("taskId", notesRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasNoCause()
                .hasMessage("Resource not found");

            verify(cftTaskDatabaseService, times(1))
                .findByIdAndObtainPessimisticWriteLock("taskId");
            verify(cftTaskDatabaseService, times(0))
                .saveTask(any());
        }

        @Test
        void should_throw_invalid_request_exception_when_notes_request_is_null() {
            NotesRequest notesRequest = null;

            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                taskManagementService.updateNotes("taskId", notesRequest));

            assertThat(exception).isInstanceOf(InvalidRequestException.class);
            assertThat(exception.getTitle()).isEqualTo("Bad Request");
            assertThat(exception.getType())
                .hasToString("https://github.com/hmcts/wa-task-management-api/problem/bad-request");

            verifyNoInteractions(cftTaskDatabaseService);
        }

        @Test
        void should_throw_custom_constraint_violation_exception_when_note_resource_is_null() {
            final NoteResource newNoteResource = null;
            List<NoteResource> newNotes = new ArrayList<>();
            newNotes.add(newNoteResource);
            NotesRequest notesRequest = new NotesRequest(newNotes);

            CustomConstraintViolationException exception = assertThrows(CustomConstraintViolationException.class, () ->
                taskManagementService.updateNotes("taskId", notesRequest));

            assertThat(exception).isInstanceOf(CustomConstraintViolationException.class);
            assertions(exception, "note_resource");

            verifyNoInteractions(cftTaskDatabaseService);
        }

        @Test
        void should_throw_custom_constraint_violation_exception_when_note_resource_code_is_null_or_empty() {
            final NoteResource newNoteResource = new NoteResource(
                "",
                "Warning",
                "userId",
                "Warning Description"
            );
            List<NoteResource> newNotes = new ArrayList<>();
            newNotes.add(newNoteResource);
            NotesRequest notesRequest = new NotesRequest(newNotes);

            CustomConstraintViolationException exception = assertThrows(CustomConstraintViolationException.class, () ->
                taskManagementService.updateNotes("taskId", notesRequest));

            assertThat(exception).isInstanceOf(CustomConstraintViolationException.class);
            assertions(exception, "code");

            verifyNoInteractions(cftTaskDatabaseService);
        }

        @Test
        void should_throw_custom_constraint_violation_exception_when_note_resource_note_type_is_null_or_empty() {
            final NoteResource newNoteResource = new NoteResource(
                "code",
                "",
                "userId",
                "Warning Description"
            );
            List<NoteResource> newNotes = new ArrayList<>();
            newNotes.add(newNoteResource);
            NotesRequest notesRequest = new NotesRequest(newNotes);

            CustomConstraintViolationException exception = assertThrows(CustomConstraintViolationException.class, () ->
                taskManagementService.updateNotes("taskId", notesRequest));

            assertThat(exception).isInstanceOf(CustomConstraintViolationException.class);
            assertions(exception, "note_type");

            verifyNoInteractions(cftTaskDatabaseService);
        }

        @Test
        void should_throw_custom_constraint_violation_exception_when_note_resource_empty() {
            NotesRequest notesRequest = new NotesRequest(emptyList());

            CustomConstraintViolationException exception = assertThrows(CustomConstraintViolationException.class, () ->
                taskManagementService.updateNotes("taskId", notesRequest));

            assertThat(exception).isInstanceOf(CustomConstraintViolationException.class);
            assertions(exception, "note_resource");

            verifyNoInteractions(cftTaskDatabaseService);
        }

        private void assertions(CustomConstraintViolationException exception, String field) {
            assertNotNull(exception.getStatus());
            assertThat(exception.getStatus().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(exception.getTitle()).isEqualTo("Constraint Violation");
            assertNotNull(exception.getViolations());
            assertThat(exception.getViolations()).hasSize(1);
            assertThat(exception.getViolations().get(0).getMessage()).isEqualTo(MUST_NOT_BE_EMPTY);
            assertThat(exception.getViolations().get(0).getField()).isEqualTo(field);
            assertThat(exception.getType())
                .hasToString("https://github.com/hmcts/wa-task-management-api/problem/constraint-validation");

        }
    }

    @Nested
    @DisplayName("getTaskRolePermissions()")
    class GetTaskRolePermissions {

        @Test
        void should_succeed() {
            taskId = "taskId";
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = new TaskResource(
                taskId, "taskName", "taskType", CFTTaskState.ASSIGNED
            );

            TaskRoleResource tribunalResource = new TaskRoleResource(
                "tribunal-caseworker", true, true, true, true, true,
                true, new String[]{"Divorce"}, 1, false, "LegalOperations"
            );

            TaskRoleResource caseManagerResource = new TaskRoleResource(
                "case-manager", true, true, true, true, true,
                true, new String[]{"Divorce"}, 1, false, "roleCategory"
            );

            TaskRoleResource withOutRead = new TaskRoleResource(
                "senior-tribunal", false, true, true, true, true,
                true, new String[]{"Divorce"}, 1, false, "roleCategory"
            );

            Set<TaskRoleResource> taskRoleResourceSet = Set.of(tribunalResource, caseManagerResource, withOutRead);
            taskResource.setTaskRoleResources(taskRoleResourceSet);

            when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.of(taskResource));
            when(entityManager.createQuery(criteriaQuery).getResultList()).thenReturn(List.of(taskResource));

            final List<TaskRolePermissions> taskRolePermissions = taskManagementService.getTaskRolePermissions(
                taskId, accessControlResponse);

            assertNotNull(taskRolePermissions);
            assertFalse(taskRolePermissions.isEmpty());

            assertEquals(3, taskRolePermissions.size());

            // first index
            TaskRolePermissions expectedRolePermission = taskRolePermissions.get(0);
            assertTrue(expectedRolePermission.getPermissions().containsAll(
                List.of(MANAGE, CANCEL, EXECUTE, OWN, READ, REFER)
            ));
            assertTrue(expectedRolePermission.getAuthorisations().contains(
                "Divorce"
            ));
            assertEquals("roleCategory", expectedRolePermission.getRoleCategory());
            assertEquals("case-manager", expectedRolePermission.getRoleName());

            // second index
            expectedRolePermission = taskRolePermissions.get(1);
            assertTrue(expectedRolePermission.getPermissions().containsAll(
                List.of(MANAGE, CANCEL, EXECUTE, OWN, REFER)
            ));
            assertTrue(expectedRolePermission.getAuthorisations().contains(
                "Divorce"
            ));
            assertEquals("roleCategory", expectedRolePermission.getRoleCategory());
            assertEquals("senior-tribunal", expectedRolePermission.getRoleName());

            // third index
            expectedRolePermission = taskRolePermissions.get(2);
            assertTrue(expectedRolePermission.getPermissions().containsAll(
                List.of(MANAGE, CANCEL, EXECUTE, OWN, READ, REFER)
            ));
            assertTrue(expectedRolePermission.getAuthorisations().contains(
                "Divorce"
            ));
            assertEquals("LegalOperations", expectedRolePermission.getRoleCategory());
            assertEquals("tribunal-caseworker", expectedRolePermission.getRoleName());

            verify(cftTaskDatabaseService, times(1)).findByIdOnly(taskId);
            verify(cftTaskMapper, times(3)).mapToTaskRolePermissions(any());
        }

        @Test
        void should_return_empty_task_role_permissions() {
            taskId = "taskId";
            TaskResource taskResource = spy(TaskResource.class);
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.of(taskResource));
            when(Optional.of(taskResource).get().getTaskRoleResources()).thenReturn(emptySet());

            final List<TaskRolePermissions> taskRolePermissions = taskManagementService.getTaskRolePermissions(
                taskId, accessControlResponse);

            assertNotNull(taskRolePermissions);
            assertTrue(taskRolePermissions.isEmpty());

            verify(cftTaskDatabaseService, times(1)).findByIdOnly(taskId);
            verify(cftTaskMapper, never()).mapToTaskRolePermissions(any());
        }

        @Test
        void should_throw_task_not_found_exception() {
            taskId = "taskId";
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> taskManagementService.getTaskRolePermissions(
                taskId, accessControlResponse));

            verify(cftTaskDatabaseService, times(1)).findByIdOnly(taskId);
            verify(cftTaskMapper, never()).mapToTaskRolePermissions(any());
        }

        @Test
        void should_throw_role_verification_exception() {
            taskId = "taskId";
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            TaskResource taskResource = spy(TaskResource.class);
            TaskRoleResource taskRoleResource = new TaskRoleResource(
                "roleName", true, true, true, true, true,
                true, new String[]{"Divorce"}, 1, false, "roleCategory"
            );

            Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
            when(Optional.of(taskResource).get().getTaskRoleResources()).thenReturn(taskRoleResourceSet);
            when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.of(taskResource));
            when(entityManager.createQuery(criteriaQuery).getResultList()).thenReturn(Collections.emptyList());

            assertThrows(RoleAssignmentVerificationException.class, () -> taskManagementService.getTaskRolePermissions(
                taskId, accessControlResponse));

            verify(cftTaskDatabaseService, times(1)).findByIdOnly(taskId);
            verify(cftTaskMapper, never()).mapToTaskRolePermissions(any());
        }

        @Test
        void should_return_granular_permission_when_feature_set() {
            taskId = "taskId";
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = new TaskResource(
                taskId, "taskName", "taskType", CFTTaskState.ASSIGNED
            );

            TaskRoleResource tribunalResource = new TaskRoleResource(
                "tribunal-caseworker", true, true, true, true, true,
                false, new String[]{"Divorce"}, 1, false, "LegalOperations",
                taskId, OffsetDateTime.now(), true, true, true, true, true, true, true, true, true, true
            );

            TaskRoleResource caseManagerResource = new TaskRoleResource(
                "case-manager", true, true, true, true, true,
                false, new String[]{"Divorce"}, 1, false, "roleCategory",
                taskId, OffsetDateTime.now(), true, true, true, true, true, true, true, true, false, true

            );

            TaskRoleResource withOutRead = new TaskRoleResource(
                "senior-tribunal", false, true, true, true, true,
                false, new String[]{"Divorce"}, 1, false, "roleCategory",
                taskId, OffsetDateTime.now(), true, true, true, true, true, true, true, true, true, false
            );

            Set<TaskRoleResource> taskRoleResourceSet = Set.of(tribunalResource, caseManagerResource, withOutRead);
            taskResource.setTaskRoleResources(taskRoleResourceSet);

            when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.of(taskResource));
            when(entityManager.createQuery(criteriaQuery).getResultList()).thenReturn(List.of(taskResource));

            final List<TaskRolePermissions> taskRolePermissions = taskManagementService.getTaskRolePermissions(
                taskId, accessControlResponse);

            assertNotNull(taskRolePermissions);
            assertFalse(taskRolePermissions.isEmpty());

            assertEquals(3, taskRolePermissions.size());

            // first index
            TaskRolePermissions expectedRolePermission = taskRolePermissions.get(0);
            assertTrue(expectedRolePermission.getPermissions().containsAll(
                List.of(MANAGE, CANCEL, EXECUTE, OWN, READ, COMPLETE, COMPLETE_OWN,
                    CANCEL_OWN, CLAIM, UNCLAIM, ASSIGN, UNASSIGN, UNCLAIM_ASSIGN, UNASSIGN_ASSIGN
                )
            ));

            // second index
            expectedRolePermission = taskRolePermissions.get(1);
            assertTrue(expectedRolePermission.getPermissions().containsAll(
                List.of(MANAGE, CANCEL, EXECUTE, OWN, COMPLETE, COMPLETE_OWN,
                    CANCEL_OWN, CLAIM, UNCLAIM, ASSIGN, UNASSIGN, UNCLAIM_ASSIGN, UNASSIGN_CLAIM
                )
            ));

            // third index
            expectedRolePermission = taskRolePermissions.get(2);
            assertTrue(expectedRolePermission.getPermissions().containsAll(
                List.of(MANAGE, CANCEL, EXECUTE, OWN, READ, COMPLETE, COMPLETE_OWN,
                    CANCEL_OWN, CLAIM, UNCLAIM, ASSIGN, UNASSIGN, UNCLAIM_ASSIGN, UNASSIGN_ASSIGN
                )
            ));
        }

        @Test
        void should_not_return_granular_permission_when_feature_set() {
            taskId = "taskId";
            userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

            TaskResource taskResource = new TaskResource(
                taskId, "taskName", "taskType", CFTTaskState.ASSIGNED
            );

            TaskRoleResource tribunalResource = new TaskRoleResource(
                "tribunal-caseworker", true, true, true, true, true,
                true, new String[]{"Divorce"}, 1, false, "LegalOperations",
                taskId, OffsetDateTime.now(), true, true, true, true, true, true, true, true, true, true
            );

            TaskRoleResource caseManagerResource = new TaskRoleResource(
                "case-manager", true, true, true, true, true,
                true, new String[]{"Divorce"}, 1, false, "roleCategory",
                taskId, OffsetDateTime.now(), true, true, true, true, true, true, true, true, true, true

            );

            TaskRoleResource withOutRead = new TaskRoleResource(
                "senior-tribunal", false, true, true, true, true,
                true, new String[]{"Divorce"}, 1, false, "roleCategory",
                taskId, OffsetDateTime.now(), true, true, true, true, true, true, true, true, true, true
            );

            Set<TaskRoleResource> taskRoleResourceSet = Set.of(tribunalResource, caseManagerResource, withOutRead);
            taskResource.setTaskRoleResources(taskRoleResourceSet);

            when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.of(taskResource));
            when(entityManager.createQuery(criteriaQuery).getResultList()).thenReturn(List.of(taskResource));

            final List<TaskRolePermissions> taskRolePermissions = taskManagementService.getTaskRolePermissions(
                taskId, accessControlResponse);

            assertNotNull(taskRolePermissions);
            assertFalse(taskRolePermissions.isEmpty());

            assertEquals(3, taskRolePermissions.size());

            // first index
            TaskRolePermissions expectedRolePermission = taskRolePermissions.get(0);
            assertTrue(expectedRolePermission.getPermissions().containsAll(
                List.of(MANAGE, CANCEL, EXECUTE, OWN, READ, REFER)
            ));

            // second index
            expectedRolePermission = taskRolePermissions.get(1);
            assertTrue(expectedRolePermission.getPermissions().containsAll(
                List.of(MANAGE, CANCEL, EXECUTE, OWN, REFER)
            ));

            // third index
            expectedRolePermission = taskRolePermissions.get(2);
            assertTrue(expectedRolePermission.getPermissions().containsAll(
                List.of(MANAGE, CANCEL, EXECUTE, OWN, READ, REFER)
            ));
            assertTrue(expectedRolePermission.getPermissions().contains(CLAIM));

        }
    }
}
