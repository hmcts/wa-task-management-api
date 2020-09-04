package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RootControllerTest {

    private final String demoSecret = "demoSecret";
    private final RootController rootController = new RootController(demoSecret);

    @Test
    public void should_return_welcome_response() {

        ResponseEntity<String> responseEntity = rootController.welcome();

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertThat(
            responseEntity.getBody(),
            containsString("Welcome to wa-task-management-api [\"demoSecret\"]")
        );
    }
}
