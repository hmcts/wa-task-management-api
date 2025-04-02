package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.consumer.ccd;

import org.mockito.Mock;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CcdDataServiceApi;

@SpringBootApplication
@EnableFeignClients(clients = {
    CcdDataServiceApi.class
})
public class CcdConsumerApplication {

    @Mock
    RestTemplate restTemplate;

}
