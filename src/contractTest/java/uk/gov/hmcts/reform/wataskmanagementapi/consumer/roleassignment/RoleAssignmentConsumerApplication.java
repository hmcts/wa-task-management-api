package uk.gov.hmcts.reform.wataskmanagementapi.consumer.roleassignment;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;


@SpringBootApplication
@EnableFeignClients(clients = {
    RoleAssignmentServiceApi.class
})
public class RoleAssignmentConsumerApplication {

    @MockitoBean
    RestTemplate restTemplate;
}
