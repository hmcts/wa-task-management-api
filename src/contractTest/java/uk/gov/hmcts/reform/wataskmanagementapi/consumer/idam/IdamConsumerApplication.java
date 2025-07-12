package uk.gov.hmcts.reform.wataskmanagementapi.consumer.idam;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;

@SpringBootApplication
@EnableFeignClients(clients = {
    IdamWebApi.class
})
public class IdamConsumerApplication {

    @MockitoBean
    AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    RestTemplate restTemplate;

}
