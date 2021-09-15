package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class RestApiActions {

    private final String baseUri;
    private final PropertyNamingStrategy propertyNamingStrategy;
    RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
    RequestSpecification specification;

    public RestApiActions(final String baseUri, final PropertyNamingStrategy propertyNamingStrategy) {
        this.baseUri = baseUri;
        this.propertyNamingStrategy = propertyNamingStrategy;
    }

    protected RequestSpecification given() {
        return RestAssured.given()
            .spec(specification)
            .config(RestAssured.config()
                        .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                            (type, s) -> {
                                ObjectMapper objectMapper = new ObjectMapper();
                                objectMapper.setPropertyNamingStrategy(propertyNamingStrategy);
                                objectMapper.registerModule(new Jdk8Module());
                                objectMapper.registerModule(new JavaTimeModule());
                                return objectMapper;
                            }
                        ))
            ).relaxedHTTPSValidation();
    }

    public RestApiActions setUp() {
        requestSpecBuilder.setBaseUri(baseUri);
        specification = requestSpecBuilder.build();
        return this;
    }

    public Response get(String path, Header header) {
        return this.get(path, null, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, new Headers(header));
    }

    public Response get(String path, String resourceId, Header header) {
        return this.get(path, resourceId, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, new Headers(header));
    }

    public Response get(String path, String resourceId, Headers headers) {
        return this.get(path, resourceId, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response get(String path, String resourceId, String accept, Headers headers) {
        return this.get(path, resourceId, APPLICATION_JSON_VALUE, accept, headers);
    }


    public Response get(String path, String resourceId, String contentType, String accept, Headers headers) {

        if (resourceId != null) {
            log.info("Calling GET {} with resource id: {}", path, resourceId);
            return given()
                .contentType(contentType)
                .accept(accept)
                .headers(headers)
                .when()
                .get(path, resourceId);
        } else {
            log.info("Calling GET {}", path);
            return given()
                .contentType(contentType)
                .accept(accept)
                .headers(headers)
                .when()
                .get(path);
        }
    }

    public Response post(String path, String resourceId, Headers headers) {
        return post(path, resourceId, null, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response post(String path, String resourceId, Object body, Header header) {
        return post(path, resourceId, body, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, new Headers(header));
    }

    public Response post(String path, String resourceId, Object body, Headers headers) {
        return post(path, resourceId, body, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response post(String path, Object body, Header header) {
        return post(path, null, body, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, new Headers(header));
    }

    public Response post(String path, Object body, Headers headers) {
        return post(path, null, body, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response post(String path,
                         String resourceId,
                         Object body,
                         String contentType,
                         String accept,
                         Headers headers) {
        return (body != null)
            ? postWithBody(path, resourceId, body, contentType, accept, headers)
            : postWithoutBody(path, resourceId, contentType, accept, headers);
    }

    public Response delete(String path, String resourceId, Headers headers) {
        return deleteWithoutBody(path, resourceId, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response delete(String path, String resourceId, Object body, Headers headers) {
        return deleteWithBody(path, resourceId, body, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public void updateFeatureFlag(String featureFlag, boolean featureToggled) {
        String accessToken = "api-0ba8bb5e-0b11-4f84-9655-93f9f8ccca94";

        final Response response = given()
            .log()
            .all()
            .contentType(APPLICATION_JSON_VALUE)
            .accept(APPLICATION_JSON_VALUE)
            .header(new Header("Authorization", accessToken))
            .body(jsonBodyWithFeatureToggled(featureToggled))
            .patch(featureFlag);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value());
    }

    private Response postWithBody(String path,
                                  String resourceId,
                                  Object body,
                                  String contentType,
                                  String accept,
                                  Headers headers) {
        if (resourceId != null) {
            log.info("Calling POST {} with resource id: {}", path, resourceId);
            return given()
                .contentType(contentType)
                .accept(accept)
                .headers(headers)
                .body(body)
                .when()
                .post(path, resourceId);
        } else {
            log.info("Calling POST {}", path);
            return given()
                .contentType(contentType)
                .accept(accept)
                .headers(headers)
                .body(body)
                .when()
                .post(path);
        }
    }

    private Response postWithoutBody(String path,
                                     String resourceId,
                                     String contentType,
                                     String accept,
                                     Headers headers) {
        if (resourceId != null) {
            log.info("Calling POST {} with resource id: {}", path, resourceId);

            return given()
                .contentType(contentType)
                .accept(accept)
                .headers(headers)
                .when()
                .post(path, resourceId);
        } else {
            log.info("Calling POST {}", path);
            return given()
                .contentType(contentType)
                .accept(accept)
                .headers(headers)
                .when()
                .post(path);
        }
    }


    private Response deleteWithBody(String path,
                                    String resourceId,
                                    Object body,
                                    String contentType,
                                    String accept,
                                    Headers headers) {
        if (resourceId != null) {
            log.info("Calling DELETE {} with resource id: {}", path, resourceId);

            return given()
                .contentType(contentType)
                .accept(accept)
                .headers(headers)
                .body(body)
                .when()
                .delete(path, resourceId);
        } else {
            log.info("Calling DELETE {}", path);
            return given()
                .contentType(contentType)
                .accept(accept)
                .headers(headers)
                .body(body)
                .when()
                .delete(path);
        }
    }

    private Response deleteWithoutBody(String path,
                                       String resourceId,
                                       String contentType,
                                       String accept,
                                       Headers headers) {
        if (resourceId != null) {
            log.info("Calling DELETE {} with resource id: {}", path, resourceId);

            return given()
                .contentType(contentType)
                .accept(accept)
                .headers(headers)
                .when()
                .delete(path, resourceId);
        } else {
            log.info("Calling DELETE {}", path);
            return given()
                .contentType(contentType)
                .accept(accept)
                .headers(headers)
                .when()
                .delete(path);
        }
    }

    private String jsonBodyWithFeatureToggled(boolean featureToggled) {
        return "["
               + " { \"op\": \"replace\", \"path\": \"/environments/test/on\", \"value\": " + featureToggled + "}\n"
               + "]";
    }

}
