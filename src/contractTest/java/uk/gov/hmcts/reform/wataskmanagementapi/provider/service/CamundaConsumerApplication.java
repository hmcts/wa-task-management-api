package uk.gov.hmcts.reform.wataskmanagementapi.provider.service;

import org.mockito.Mock;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;

@EnableFeignClients(clients = {
    CamundaServiceApi.class
})
public class CamundaConsumerApplication {
    @Mock
    RestTemplate restTemplate;
}

