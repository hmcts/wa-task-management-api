package uk.gov.hmcts.reform.wataskmanagementapi;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static net.serenitybdd.rest.SerenityRest.expect;
import static org.hamcrest.Matchers.containsString;

public class WelcomeTest extends SpringBootFunctionalBaseTest {

    @Value("${targets.instance}")
    private String testUrl;

    @Before
    public void setUp() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.with().contentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    public void should_welcome_with_200_response_code() {

        expect()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body(containsString("Welcome to wa-task-management-api"))
            .when()
            .get("/");
    }
}
