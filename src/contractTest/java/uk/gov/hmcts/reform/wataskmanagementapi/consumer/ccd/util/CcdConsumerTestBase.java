package uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd.util;

import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.annotations.PactFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd.CoreCaseDataConsumerApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ContextConfiguration(classes = {CoreCaseDataConsumerApplication.class})
@PactTestFor(providerName = "ccdDataStoreAPI_Cases", port = "8891")
@PactFolder("pacts")
@SuppressWarnings("unchecked")
public abstract class CcdConsumerTestBase extends SpringBootContractBaseTest {

    public static final String JURISDICTION = "jurisdictionId";
    public static final String CASE_TYPE = "caseType";
    public static final String CASE_DATA_CONTENT = "caseDataContent";
    public static final String EVENT_ID = "eventId";
    public static final String SUBMIT_APPEAL = "submitAppeal";
    public static final String START_APPEAL = "startAppeal";
    protected static final String USER_ID = "123456";
    protected static final Long CASE_ID = 1593694526480000L;

    protected Map<String, Object> caseDetailsMap;

    protected CaseDataContent caseDataContent;

    @Autowired
    protected CoreCaseDataApi coreCaseDataApi;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> getCaseDetailsAsMap(String fileName) throws JSONException, IOException {
        File file = getFile(fileName);
        CaseDetails caseDetails = objectMapper.readValue(file, CaseDetails.class);
        Map<String, Object> map = objectMapper.convertValue(caseDetails, Map.class);
        return map;
    }

    public Map<String, Object> setUpStateMapForProviderWithCaseData(CaseDataContent caseDataContent)
        throws JSONException {
        Map<String, Object> map = this.setUpStateMapForProviderWithoutCaseData();
        Map<String, Object> caseDataContentMap = objectMapper.convertValue(caseDataContent, Map.class);
        map.put(CASE_DATA_CONTENT, caseDataContentMap);
        return map;
    }

    public Map<String, Object> setUpStateMapForProviderWithoutCaseData() {
        Map<String, Object> map = new HashMap<>();
        map.put(JURISDICTION, "IA");
        map.put(CASE_TYPE, "Asylum");
        return map;
    }

    public String convertObjectToJsonString(final Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    public void setUp() throws Exception {
        caseDetailsMap = getCaseDetailsAsMap("case_data_map.json");
        caseDataContent = createCaseDataContent(START_APPEAL, caseDetailsMap);
    }

    public CaseDataContent createCaseDataContent(String eventId, Map<String, Object> caseDetailsMap) {
        return CaseDataContent.builder()
            .eventToken("someEventToken")
            .event(
                Event.builder()
                    .id(eventId)
                    .summary("summary")
                    .description("description")
                    .build()
            ).data(this.caseDetailsMap.get("case_data"))
            .build();
    }

    private File getFile(String fileName) throws FileNotFoundException {
        return ResourceUtils.getFile(this.getClass().getResource("/json/" + fileName));
    }

}
