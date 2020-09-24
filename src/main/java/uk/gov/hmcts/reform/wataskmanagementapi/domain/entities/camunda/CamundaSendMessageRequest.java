package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import lombok.Data;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.Builder;

import java.util.Map;

@Data
public class CamundaSendMessageRequest {

    private String messageName;
    private Map<String, CamundaValue<?>> processVariables;

    public CamundaSendMessageRequest(String messageName, Map<String, CamundaValue<?>> processVariables) {
        this.messageName = messageName;
        this.processVariables = processVariables;
    }

    public static class SendMessageBuilder implements Builder<CamundaSendMessageRequest> {

        private String messageName;
        private Map<String, CamundaValue<?>> processVariables;

        public static SendMessageBuilder sendCamundaMessageRequest() {
            return new SendMessageBuilder();
        }

        public SendMessageBuilder withMessageName(String messageName) {
            this.messageName = messageName;
            return this;
        }

        public SendMessageBuilder withProcessVariables(Map<String, CamundaValue<?>> processVariables) {
            this.processVariables = processVariables;
            return this;
        }

        @Override
        public CamundaSendMessageRequest build() {
            return new CamundaSendMessageRequest(messageName, processVariables);
        }
    }
}

