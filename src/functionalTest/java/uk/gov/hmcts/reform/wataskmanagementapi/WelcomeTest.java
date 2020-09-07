package uk.gov.hmcts.reform.wataskmanagementapi;

import io.restassured.RestAssured;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.containsString;

@RunWith(SpringIntegrationSerenityRunner.class)
@ActiveProfiles("functional")
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.LawOfDemeter", "PMD.BeanMembersShouldSerialize"})
public class WelcomeTest {

    private final String testUrl = System.getenv("TEST_URL") == null ? "http://localhost:8090" : System.getenv("TEST_URL");

    @Before
    public void setUp() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void should_welcome_with_200_response_code() {

        SerenityRest.given()
            .when()
            .get("/")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(containsString("Welcome to wa-task-management-api"));
    }
}
