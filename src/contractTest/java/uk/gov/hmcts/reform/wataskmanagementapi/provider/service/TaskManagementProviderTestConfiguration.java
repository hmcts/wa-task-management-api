package uk.gov.hmcts.reform.wataskmanagementapi.provider.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaErrorDecoder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskMapper;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TaskManagementProviderTestConfiguration {

    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private CamundaErrorDecoder camundaErrorDecoder;
    @MockBean
    private CamundaQueryBuilder camundaQueryBuilder;
    @MockBean
    private TaskMapper taskMapper;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private PermissionEvaluatorService permissionEvaluatorService;
    @MockBean
    private CamundaObjectMapper camundaObjectMapper;

    @MockBean
    private IdamService idamService;

    @MockBean
    private RoleAssignmentService roleAssignmentService;

    @Bean
    @Primary
    public CamundaService camundaService() {
        return new CamundaService(
            camundaServiceApi,
            camundaQueryBuilder,
            camundaErrorDecoder,
            taskMapper,
            authTokenGenerator,
            permissionEvaluatorService,
            camundaObjectMapper
        );
    }

    @Bean
    @Primary
    public AccessControlService accessControlService() {
        return new AccessControlService(
            idamService,
            roleAssignmentService
        );
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.setDateFormat(df);
        return objectMapper;
    }


}
