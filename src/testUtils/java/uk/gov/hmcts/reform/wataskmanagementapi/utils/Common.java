package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CcdIdGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

public class Common {

    private final CcdIdGenerator ccdIdGenerator;
    private final GivensBuilder given;

    public Common(CcdIdGenerator ccdIdGenerator, GivensBuilder given) {
        this.ccdIdGenerator = ccdIdGenerator;
        this.given = given;
    }

    public Map<String, String> setupTaskAndRetrieveIds() {
        String ccdId = ccdIdGenerator.generate();

        List<CamundaTask> response = given
            .iCreateATaskWithCcdId(ccdId)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("ccdId", ccdId);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task:" + "used:" + ccdId);
        }

        new HashMap<>();

        return Map.of(
            "ccdId", ccdId,
            "taskId", response.get(0).getId()
        );

    }
}
