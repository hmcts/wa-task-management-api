package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskMapperTest {

    ObjectMapper mapper;
    private TaskMapper taskMapper;

    @BeforeEach
    public void setUp() {
        taskMapper = new TaskMapper();
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void should_map_to_task_object() throws JsonProcessingException {

        //String camundaTaskJson = "{\"id\": \"7f83544b-fe60-11ea-bdab-0242ac110013\",\n"
        //                         + "  \"name\": \"Process Task\",\n"
        //                         + "  \"assignee\": \"show-and-tell-user\",\n"
        //                         + "  \"created\": \"2020-09-24T12:21:39.785+0000\",\n"
        //                         + "  \"due\": \"2020-09-26T09:49:14.739+0000\"}";
        //CamundaTask camundaTask = mapper.readValue(camundaTaskJson, CamundaTask.class);
        //String variablesJson = "{\n"
        //                       + "  \"state\": {\n"
        //                       + "    \"type\": \"String\",\n"
        //                       + "    \"value\": \"someState\"\n"
        //                       + "  },\n"
        //                       + "  \"staffLocationId\": {\n"
        //                       + "    \"type\": \"String\",\n"
        //                       + "    \"value\": \"someStaffLocationId\"\n"
        //                       + "  },\n"
        //                       + "  \"staffLocation\": {\n"
        //                       + "    \"type\": \"String\",\n"
        //                       + "    \"value\": \"someStaffLocation\"\n"
        //                       + "  },\n"
        //                       + "  \"ccdId\": {\n"
        //                       + "    \"type\": \"String\",\n"
        //                       + "    \"value\": \"someCcdId\"\n"
        //                       + "  },\n"
        //                       + "  \"caseName\": {\n"
        //                       + "    \"type\": \"String\",\n"
        //                       + "    \"value\": \"someCaseName\"\n"
        //                       + "  },\n"
        //                       + "  \"caseType\": {\n"
        //                       + "    \"type\": \"String\",\n"
        //                       + "    \"value\": \"someCasetype\"\n"
        //                       + "  }\n"
        //                       + "}";
        //
        //Map<String, CamundaVariable> variables = mapper.readValue(variablesJson, Map.class);
        //
        //Task result = taskMapper.mapToTaskObject(camundaTask, variables);
        //
        //assertNotNull(result);
        //assertEquals("someState", result.getState());
        //assertEquals("2020-09-26T09:49:14.739+0000", result.getDueDate());
        //assertEquals("Process Task", result.getName());
        //assertNotNull(result.getAssignee());
        //assertEquals("id", result.getAssignee().getId());
        //assertEquals("someUsername", result.getAssignee().getUserName());
        //assertNotNull(result.getCaseData());
        //assertEquals("", result.getCaseData().getName());
        //assertEquals("", result.getCaseData().getCategory());
        //assertEquals("", result.getCaseData().getReference());
        //assertNotNull(result.getCaseData().getLocation());
        //assertEquals("", result.getCaseData().getLocation().getId());
        //assertEquals("", result.getCaseData().getLocation().getLocation());


    }
}
