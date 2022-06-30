package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Slf4j
public class RoleAssignmentHelper {

    @Value("${role-assignment-service.url}")
    protected String roleAssignmentUrl;

    @Autowired
    private AuthTokenGenerator serviceAuthTokenGenerator;

    @Autowired
    private IdamTokenGenerator systemUserIdamToken;

    public void setRoleAssignments(String caseId) throws IOException {
        String bearerUserToken = systemUserIdamToken.generate();
        String s2sToken = serviceAuthTokenGenerator.generate();
        UserInfo userInfo = systemUserIdamToken.getUserInfo(bearerUserToken);
        createRoleAssignmentInThisOrder(caseId, bearerUserToken, s2sToken, userInfo);
        log.info("Role Assignments created successfully");
    }

    private void createRoleAssignmentInThisOrder(String caseId,
                                                 String bearerUserToken,
                                                 String s2sToken,
                                                 UserInfo userInfo) throws IOException {

        log.info("Creating Organizational Role...");
        postRoleAssignment(
            caseId,
            bearerUserToken,
            s2sToken,
            userInfo,
            "set-rules-assignment-request.json"
        );

        log.info("Creating Restricted role-assignment...");
        postRoleAssignment(
            caseId,
            bearerUserToken,
            s2sToken,
            userInfo,
            "assignment-request.json"
        );
    }

    private void postRoleAssignment(String caseId,
                                    String bearerUserToken,
                                    String s2sToken,
                                    UserInfo userInfo,
                                    String resourceFilename) throws IOException {

        given()
            .relaxedHTTPSValidation()
            .contentType(APPLICATION_JSON_VALUE)
            .header("ServiceAuthorization", s2sToken)
            .header("Authorization", bearerUserToken)
            .baseUri(roleAssignmentUrl)
            .basePath("/am/role-assignments")
            .body(getBody(caseId, userInfo, resourceFilename))
            .when()
            .post()
            .prettyPeek()
            .then()
            .statusCode(HttpStatus.CREATED_201);
    }

    @NotNull
    private String getBody(final String caseId,
                           final UserInfo userInfo,
                           final String resourceFilename) throws IOException {
        String assignmentRequestBody =
            FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:" + resourceFilename),
                StandardCharsets.UTF_8
            );

        assignmentRequestBody = assignmentRequestBody.replace("{ACTOR_ID_PLACEHOLDER}", userInfo.getUid());
        assignmentRequestBody = assignmentRequestBody.replace("{CASE_ID_PLACEHOLDER}", caseId);
        assignmentRequestBody = assignmentRequestBody.replace("{ASSIGNER_ID_PLACEHOLDER}", userInfo.getUid());

        return assignmentRequestBody;
    }

}
