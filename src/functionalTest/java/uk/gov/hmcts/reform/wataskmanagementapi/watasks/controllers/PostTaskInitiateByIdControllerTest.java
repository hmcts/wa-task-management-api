package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest2;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.CaseConfigurationProviderService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.jsonValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.CaseConfigurationProviderService.ADDITIONAL_PROPERTIES_KEY;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.CaseConfigurationProviderService.ADDITIONAL_PROPERTIES_PREFIX;

public class PostTaskInitiateByIdControllerTest extends SpringBootFunctionalBaseTest {
    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";

    private TestAuthenticationCredentials caseworkerCredentials;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    CamundaServiceApi camundaServiceApi;

    @Autowired
    CaseConfigurationProviderService caseConfigurationProviderService;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewWaTribunalCaseworker("wa-ft-test-r2");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_201_when_initiating_a_process_application_task_by_id() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "processApplication",
                TASK_NAME.value(), "Process Application",
                TASK_CASE_ID.value(), taskVariables.getCaseId(),
                TASK_TITLE.value(), "Process Application",
                TASK_CREATED.value(), formattedCreatedDate,
                TASK_DUE_DATE.value(), formattedDueDate
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );

        //Note: this is the TaskResource.class
        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("Process Application"))
            .body("task_type", equalTo("processApplication"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("Process Application"))
            .body("created", notNullValue())
            .body("due_date_time", notNullValue())
            .body("auto_assigned", equalTo(false))
            .body("has_warnings", equalTo(false))
            .body("case_id", equalTo(taskVariables.getCaseId()))
            .body("case_type_id", equalTo("WaCaseType"))
            .body("jurisdiction", equalTo("WA"))
            .body("region", equalTo("1"))
            .body("location", equalTo("765324"))
            .body("location_name", equalTo("Taylor House"))
            .body("execution_type_code.execution_code", equalTo("CASE_EVENT"))
            .body("execution_type_code.execution_name", equalTo("Case Management Task"))
            .body(
                "execution_type_code.description",
                equalTo("The task requires a case management event to be executed by the user. "
                            + "(Typically this will be in CCD.)")
            )
            .body("work_type_resource.id", equalTo("hearing_work"))
            .body("work_type_resource.label", equalTo("Hearing work"))
            .body("role_category", equalTo("LEGAL_OPERATIONS"))
            .body("description", equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                             + "trigger/decideAnApplication)"))
            .body("task_role_resources.size()", equalTo(10))
            .body("additional_properties", equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            )));

        assertPermissions(
            getTaskResource(result, "task-supervisor"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", true),
                entry("execute", false),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("auto_assignable", false)
            )
        );


        assertPermissions(
            getTaskResource(result, "senior-tribunal-caseworker"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", false),
                entry("execute", true),
                entry("cancel", false),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "LEGAL_OPERATIONS"),
                entry("auto_assignable", false),
                entry("assignment_priority", 2)
            )
        );
        assertPermissions(
            getTaskResource(result, "tribunal-caseworker"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", false),
                entry("execute", true),
                entry("cancel", false),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "LEGAL_OPERATIONS"),
                entry("auto_assignable", false),
                entry("assignment_priority", 2)
            )
        );

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_specific_access_task_by_id() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "reviewSpecificAccessRequestJudiciary",
                TASK_NAME.value(), "additionalProperties_roleAssignmentId",
                TASK_CASE_ID.value(), taskVariables.getCaseId(),
                TASK_TITLE.value(), "Specific Access Task",
                TASK_CREATED.value(), formattedCreatedDate,
                TASK_DUE_DATE.value(), formattedDueDate
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );

        //Note: this is the TaskResource.class
        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("additionalProperties_roleAssignmentId"))
            .body("task_type", equalTo("reviewSpecificAccessRequestJudiciary"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("additionalProperties_roleAssignmentId"))
            .body("created", notNullValue())
            .body("due_date_time", notNullValue())
            .body("auto_assigned", equalTo(false))
            .body("has_warnings", equalTo(false))
            .body("case_id", equalTo(taskVariables.getCaseId()))
            .body("case_type_id", equalTo("WaCaseType"))
            .body("jurisdiction", equalTo("WA"))
            .body("region", equalTo("1"))
            .body("location", equalTo("765324"))
            .body("location_name", equalTo("Taylor House"))
            .body("execution_type_code.execution_code", equalTo("CASE_EVENT"))
            .body("execution_type_code.execution_name", equalTo("Case Management Task"))
            .body(
                "execution_type_code.description",
                equalTo("The task requires a case management event to be executed by the user. "
                            + "(Typically this will be in CCD.)")
            )
            .body("task_role_resources.size()", equalTo(9))
            .body("additional_properties", equalToObject(Map.of(
                "roleAssignmentId", "roleAssignmentId")));

        assertPermissions(
            getTaskResource(result, "judge"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", true),
                entry("manage", false),
                entry("execute", false),
                entry("cancel", false),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("auto_assignable", true)
            )
        );

        assertPermissions(
            getTaskResource(result, "task-supervisor"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", true),
                entry("execute", false),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("auto_assignable", false)
            )
        );

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_process_application_task_by_id_V2() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        ///
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        ZonedDateTime assignmentExpiry = createdDate.plusDays(1);
        String assignmentExpiryDate = CAMUNDA_DATA_TIME_FORMATTER.format(assignmentExpiry);

        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", "12345678",
            "key1", "value1",
            "key2", "value2",
            "key3", "value3",
            "key4", "value4",
            "key5", "value5",
            "key6", "value6",
            "key7", "value7",
            "key8", "value8"
        );

        WarningValues warningValues = new WarningValues(
            asList(
                new Warning("Code1", "Text1"),
                new Warning("Code2", "Text2")
            ));


        TaskRoleResource tribunalResource = new TaskRoleResource(
            "tribunal-caseworker", true, true, true, true, true,
            true, new String[]{}, 1, false, "LegalOperations"
        );
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(tribunalResource);

        Map<String, Object> attributes = new HashMap<>();
        //exceptional case
        //attributes.put("notes", warningValues);

        attributes.put("taskId", taskId);
        attributes.put("taskType", "processApplication");
        attributes.put("caseId", taskVariables.getCaseId());

        attributes.put("taskName", "task name");
        attributes.put("state", CFTTaskState.UNCONFIGURED);
        attributes.put("created", formattedCreatedDate);
        attributes.put("dueDateTime", formattedDueDate);
        attributes.put("additionalProperties", additionalProperties);
        attributes.put("key8", "value8");
        attributes.put("taskSystem", TaskSystem.SELF);
        attributes.put("securityClassification", SecurityClassification.PRIVATE);
        attributes.put("title", "a task title");
        attributes.put("description", "a task description");
        attributes.put("majorPriority", 1);
        attributes.put("minorPriority", 100);
        attributes.put("assignee", "some assignee");
        attributes.put("autoAssigned", true);
        attributes.put(
            "executionTypeCode",
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description")
        );
        attributes.put("workTypeResource", new WorkTypeResource("routine_work", "Routine work"));
        attributes.put("roleCategory", RoleCategory.LEGAL_OPERATIONS.name());
        attributes.put("hasWarnings", false);
        attributes.put("assignmentExpiry", assignmentExpiryDate);
        attributes.put("caseTypeId", "WaCaseType");
        attributes.put("jurisdiction", "WA");
        attributes.put("region", "a region");
        attributes.put("regionName", "a region name");
        attributes.put("location", "a location");
        attributes.put("locationName", "a location name");
        attributes.put("businessContext", BusinessContext.CFT_TASK);
        attributes.put("terminationReason", "a termination reason");
        attributes.put("taskRoleResources", taskRoleResourceSet);
        attributes.put("caseCategory", "a case category");

        InitiateTaskRequest2 initiateTaskRequest = new InitiateTaskRequest2(INITIATION, attributes);
        ///

        Response result = restApiActions.post(
            "task/v2/{task-id}",
            taskId,
            initiateTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        //Note: this is the TaskResource.class
        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("process Application"))
            .body("task_type", equalTo("processApplication"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("process Application"))
            .body("created", notNullValue())
            .body("due_date_time", notNullValue())
            .body("auto_assigned", equalTo(false))
            .body("has_warnings", equalTo(false))
            .body("case_id", equalTo(taskVariables.getCaseId()))
            .body("case_type_id", equalTo("WaCaseType"))
            .body("jurisdiction", equalTo("WA"))
            .body("region", equalTo("1"))
            .body("location", equalTo("765324"))
            .body("location_name", equalTo("Taylor House"))
            .body("execution_type_code.execution_code", equalTo("CASE_EVENT"))
            .body("execution_type_code.execution_name", equalTo("Case Management Task"))
            .body(
                "execution_type_code.description",
                equalTo("The task requires a case management event to be executed by the user. "
                            + "(Typically this will be in CCD.)")
            )
            .body("work_type_resource.id", equalTo("hearing_work"))
            .body("work_type_resource.label", equalTo("Hearing work"))
            .body("role_category", equalTo("LEGAL_OPERATIONS"))
            .body("description", equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                             + "trigger/decideAnApplication)"))
            .body("task_role_resources.size()", equalTo(10))
            .body("additional_properties", equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            )));

        assertPermissions(
            getTaskResource(result, "task-supervisor"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", true),
                entry("execute", false),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("auto_assignable", false)
            )
        );


        assertPermissions(
            getTaskResource(result, "senior-tribunal-caseworker"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", false),
                entry("execute", true),
                entry("cancel", false),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "LEGAL_OPERATIONS"),
                entry("auto_assignable", false),
                entry("assignment_priority", 2)
            )
        );
        assertPermissions(
            getTaskResource(result, "tribunal-caseworker"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", false),
                entry("execute", true),
                entry("cancel", false),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "LEGAL_OPERATIONS"),
                entry("auto_assignable", false),
                entry("assignment_priority", 2)
            )
        );

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void map_test() {

        String decisionTableKey = "wa-task-configuration-wa-wacasetype";
        String token = caseworkerCredentials.getHeaders().getValue("ServiceAuthorization");
        String jurisdiction = "WA";

        //todo: part-1

        List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResults =
            camundaServiceApi.evaluateConfigurationDmnTable(
                token,
                decisionTableKey,
                jurisdiction.toLowerCase(Locale.ROOT),
                new DmnRequest<>(new DecisionTableRequest(jsonValue(getCcdData()), jsonValue(getTaskAttributes())))
            );

        List<Map<String, CamundaValue>> taskConfigurationDmnResults2 = camundaServiceApi.evaluateConfigurationDmnTable2(
            token,
            decisionTableKey,
            jurisdiction.toLowerCase(Locale.ROOT),
            new DmnRequest<>(new DecisionTableRequest(jsonValue(getCcdData()), jsonValue(getTaskAttributes())))
        );

        System.out.println("compare 2 two response");
        //todo: compare 2 two response
        //taskConfigurationDmnResults
        //taskConfigurationDmnResults2
        //ikisi de degerleri donuyor yeni olan name ve value olarak ayri ayri donuyor map.get("name") map.get("value") seklinde alinir


        //todo: part-2
        List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResultsWithAdditionalProperties =
            updateTaskConfigurationDmnResultsForAdditionalProperties(taskConfigurationDmnResults);

        List<Map<String, CamundaValue>> taskConfigurationDmnResultsWithAdditionalProperties2 =
            updateTaskConfigurationDmnResultsForAdditionalProperties2(taskConfigurationDmnResults2);

        System.out.println("compare 2 two response");
        //todo: compare 2 responses
        //taskConfigurationDmnResultsWithAdditionalProperties
        // taskConfigurationDmnResultsWithAdditionalProperties2

    }

    private List<ConfigurationDmnEvaluationResponse> updateTaskConfigurationDmnResultsForAdditionalProperties(
        List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResults) {

        Map<String, Object> additionalProperties = taskConfigurationDmnResults.stream()
            .filter(r -> r.getName().getValue().contains(ADDITIONAL_PROPERTIES_PREFIX))
            .map(this::removeAdditionalFromCamundaName)
            .collect(toMap(r -> r.getName().getValue(), r -> r.getValue().getValue()));

        List<ConfigurationDmnEvaluationResponse> configResponses = taskConfigurationDmnResults.stream()
            .filter(r -> !r.getName().getValue().contains(ADDITIONAL_PROPERTIES_PREFIX)).collect(Collectors.toList());

        if (!additionalProperties.isEmpty()) {
            configResponses.add(new ConfigurationDmnEvaluationResponse(
                CamundaValue.stringValue(ADDITIONAL_PROPERTIES_KEY),
                CamundaValue.stringValue(writeValueAsString(additionalProperties))
            ));
        }
        return configResponses;
    }

    private List<Map<String, CamundaValue>> updateTaskConfigurationDmnResultsForAdditionalProperties2(
        List<Map<String, CamundaValue>> taskConfigurationDmnResults) {

        List<Map<String, CamundaValue>> cleanedTaskConfigurationDmnResults = new ArrayList<>();

        Iterator<Map<String, CamundaValue>> iterator = taskConfigurationDmnResults.iterator();

        Map<String, CamundaValue> additionalPropertiesMap = new HashMap<>();
        Map<String, CamundaValue> item;

        Map<String, CamundaValue> additionalProperties = new HashMap<>();

        while (iterator.hasNext()) {
            item = iterator.next();
            if (!item.get("name").getValue().toString().contains(ADDITIONAL_PROPERTIES_PREFIX)) {
                cleanedTaskConfigurationDmnResults.add(item);
                continue;
            }
            additionalPropertiesMap.putAll(removeAdditionalFromCamundaName(item));
        }

        //todo: add additional property map
        additionalProperties.put(ADDITIONAL_PROPERTIES_KEY, readValue(writeValueAsString2(additionalPropertiesMap)));
        cleanedTaskConfigurationDmnResults.add(additionalProperties);

        //todo: bir tane additional_properties key'ne value olarak map ata
        if (!additionalPropertiesMap.isEmpty()) {
            //cleanedTaskConfigurationDmnResults.add(
            //    new ConfigurationDmnEvaluationResponse(
            //        CamundaValue.stringValue(ADDITIONAL_PROPERTIES_KEY),
            //        CamundaValue.stringValue(writeValueAsString(additionalProperties)
            //        )
            //    ));
        }
        return cleanedTaskConfigurationDmnResults;
    }

    private Map<String, CamundaValue> removeAdditionalFromCamundaName(Map<String, CamundaValue> resp) {

        Map<String, CamundaValue> additionalPropertiesMap = new HashMap<>();

        String additionalPropertyKey = resp.get("name").getValue().toString()
            .replace(ADDITIONAL_PROPERTIES_PREFIX, "");

        CamundaValue additionalPropertyValue = readValue(resp.get("value"));

        additionalPropertiesMap.put(additionalPropertyKey, additionalPropertyValue);

        return additionalPropertiesMap;
    }

    private String writeValueAsString(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            System.out.println("Case Configuration : Could not extract case data");
        }
        return null;
    }

    private String writeValueAsString2(Map<String, CamundaValue> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            System.out.println("Case Configuration : Could not extract case data");
        }
        return null;
    }

    private CamundaValue readValue(Object data) {
        try {
            return objectMapper.convertValue(data, new TypeReference<CamundaValue>() {
            });
        } catch (Exception e) {
            System.out.println("Case Configuration : Could not extract case data");
        }
        return null;
    }


    private void assertPermissions(Map<String, Object> resource, Map<String, Object> expectedPermissions) {
        expectedPermissions.keySet().forEach(key ->
                                                 assertThat(resource).containsEntry(key, expectedPermissions.get(key)));

        assertThat(resource.get("task_role_id")).isNotNull();
    }

    private Map<String, Object> getTaskResource(Response result, String roleName) {
        final List<Map<String, Object>> resources = new JsonPath(result.getBody().asString())
            .param("roleName", roleName)
            .get("task_role_resources.findAll { resource -> resource.role_name == roleName }");
        return resources.size() > 0 ? resources.get(0) : null;
    }

    private DmnRequest getBody() {
        return new DmnRequest<>(new DecisionTableRequest(jsonValue(getCcdData()), jsonValue(getTaskAttributes())));
    }

    @NotNull
    private String getCcdData() {
        return "{"
            + "\"jurisdiction\": \"wa\","
            + "\"case_type_id\": \"WaCaseType\","
            + "\"security_classification\": \"PUBLIC\","
            + "\"data\": {}"
            + "}";
    }

    @NotNull
    private String getTaskAttributes() {
        return "{\n"
            + "    \"taskType\": \"processApplication\"\n"
            + "}";
    }


    private ConfigurationDmnEvaluationResponse removeAdditionalFromCamundaName(
        ConfigurationDmnEvaluationResponse resp) {
        String additionalPropKey = resp.getName().getValue().replace(ADDITIONAL_PROPERTIES_PREFIX, "");
        return new ConfigurationDmnEvaluationResponse(CamundaValue.stringValue(additionalPropKey), resp.getValue());
    }

}
