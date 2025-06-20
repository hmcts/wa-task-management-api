package uk.gov.hmcts.reform.wataskmanagementapi.provider.service;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;

@EnableFeignClients(clients = {
    CamundaServiceApi.class
})
public class CamundaConsumerApplication {
    @MockitoBean
    RestTemplate restTemplate;
}

