package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.Builder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CamundaLocalVariables {

    Map<String, CamundaValue<String>> localVariablesMap;

    public CamundaLocalVariables(Map<String, CamundaValue<String>> localVariablesMap) {
        this.localVariablesMap = localVariablesMap;
    }

    public Map<String, CamundaValue<String>> getLocalVariablesMap() {
        return localVariablesMap;
    }

    public static class LocalVariablesBuilder implements Builder<CamundaLocalVariables> {

        Map<String, CamundaValue<String>> localVariablesMap = new ConcurrentHashMap<>();

        public static LocalVariablesBuilder localVariables() {
            return new LocalVariablesBuilder();
        }

        public LocalVariablesBuilder withLocalVariable(String key, String value) {
            localVariablesMap.put(key, CamundaValue.stringValue(value));
            return this;
        }

        @Override
        public CamundaLocalVariables build() {
            return new CamundaLocalVariables(localVariablesMap);
        }
    }
}

