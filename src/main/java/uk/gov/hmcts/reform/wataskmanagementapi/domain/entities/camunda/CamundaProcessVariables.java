package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.Builder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;

public class CamundaProcessVariables {

    Map<String, CamundaValue<?>> processVariablesMap;

    public CamundaProcessVariables(Map<String, CamundaValue<?>> processVariablesMap) {
        this.processVariablesMap = processVariablesMap;
    }

    public Map<String, CamundaValue<?>> getProcessVariablesMap() {
        return processVariablesMap;
    }

    public static class ProcessVariablesBuilder implements Builder<CamundaProcessVariables> {

        Map<String, CamundaValue<?>> processVariablesMap = new ConcurrentHashMap<>();

        public static ProcessVariablesBuilder processVariables() {
            return new ProcessVariablesBuilder();
        }

        public ProcessVariablesBuilder withProcessVariable(String key, String value) {
            processVariablesMap.put(key, stringValue(value));
            return this;
        }

        public ProcessVariablesBuilder withProcessVariablBooleane(String key, boolean value) {
            processVariablesMap.put(key, booleanValue(value));
            return this;
        }

        @Override
        public CamundaProcessVariables build() {
            return new CamundaProcessVariables(processVariablesMap);
        }
    }
}

