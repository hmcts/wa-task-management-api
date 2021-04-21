package uk.gov.hmcts.reform.wataskmanagementapi.provider.service;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
@EnableFeignClients(clients = {
    CamundaServiceApi.class
})
public class CamundaConsumerApplication {
    @MockBean
    RestTemplate restTemplate;
}

