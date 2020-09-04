package uk.gov.hmcts.reform.wataskconfigurationapi;

import io.restassured.RestAssured;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.containsString;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("functional")
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.LawOfDemeter", "PMD.BeanMembersShouldSerialize"})
public class WelcomeTest {

    @Value("${targetInstance}") private String testUrl;

    @LocalServerPort private int port;

    @Before
    public void setUp() {

        if ("http://localhost".equals(testUrl)) {
            RestAssured.port = port;
        } else {
            RestAssured.baseURI = testUrl;
        }
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
