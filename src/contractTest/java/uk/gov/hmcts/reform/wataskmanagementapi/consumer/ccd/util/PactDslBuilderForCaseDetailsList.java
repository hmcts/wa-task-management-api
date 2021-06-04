package uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd.util;

import au.com.dius.pact.consumer.dsl.DslPart;
import io.pactfoundation.consumer.dsl.LambdaDslObject;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;

public final class PactDslBuilderForCaseDetailsList {

    public static final String REGEX_DATE = "^((19|2[0-9])[0-9]{2})-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$";
    private static final String ALPHABETIC_REGEX = "[/^[A-Za-z_]+$/]+";

    public static DslPart buildStartEventForCaseWorkerPactDsl(String eventId) {
        return newJsonBody(o ->
            o.stringType("event_id", eventId)
                .stringType("token", null)
                .object("case_details", cd -> {
                    cd.numberType("id", 2000);
                    cd.stringMatcher("jurisdiction", ALPHABETIC_REGEX, "IA");
                    cd.stringMatcher("case_type", ALPHABETIC_REGEX, "Asylum");
                    cd.stringValue("state", "appealStarted");
                    cd.stringValue("securityClassification", "PUBLIC");
                    cd.object("case_data", PactDslBuilderForCaseDetailsList::getCaseDataPactDsl);
                })).build();
    }

    public static DslPart buildStartForCaseWorkerPactDsl(String eventId) {
        return newJsonBody(
            o -> o.stringType("event_id", eventId)
                .stringType("token", null)
                .object("case_details", cd -> {
                    cd.stringMatcher("jurisdiction", ALPHABETIC_REGEX, "IA");
                    cd.stringMatcher("case_type_id", ALPHABETIC_REGEX, "Asylum");
                    cd.object("case_data", data -> {
                        data.stringMatcher("isOutOfCountryEnabled", "Yes|No|YES|NO", "No");
                        data.stringMatcher("appealOutOfCountry", "Yes|No|YES|NO", "No");
                    });
                }))
            .build();
    }

    public static DslPart buildSubmitForCaseWorkedPactDsl(Long caseId) {
        return newJsonBody(
            o -> o.numberType("id", caseId)
                .stringType("jurisdiction", "IA")
                .stringValue("case_type_id", "Asylum")
                .stringValue("state", "appealStarted")
                .stringValue("securityClassification", "PUBLIC")
                .stringValue("callbackResponseStatus", "CALLBACK_COMPLETED")
                .object("case_data", PactDslBuilderForCaseDetailsList::getCaseDataPactDsl)).build();
    }

    private static void getCaseDataPactDsl(final LambdaDslObject dataMap) {
        dataMap
            .stringType("homeOfficeReferenceNumber", "000123456")
            .stringMatcher("submissionOutOfTime", "Yes|No|YES|NO", "Yes")
            .stringMatcher("homeOfficeDecisionDate", REGEX_DATE, "2019-08-01")
            .stringMatcher("appellantDateOfBirth", REGEX_DATE, "1990-12-07")
            .stringType("appellantTitle", "Mr")
            .stringType("appellantNameForDisplay", "Bob Smith")
            .stringType("appellantFamilyName", "Smith")
            .stringType("appellantGivenNames", "Bob")
            .stringMatcher("uploadAdditionalEvidenceActionAvailable", "Yes|No|YES|NO", "No")
            .stringType("appealType", "protection")
            .stringMatcher("appealReferenceNumber", "DRAFT")
            .stringType("applicationOutOfTimeExplanation", "test case")
            .stringType("legalRepCompanyName", "")
            .stringType("staffLocation", "Taylor House")
            .stringType("currentCaseStateVisibleToLegalRepresentative", "appealStarted")
            .stringType("uploadAddendumEvidenceLegalRepActionAvailable", "No")
            .object("legalRepCompanyAddress", addr -> {
                addr.stringType("AddressLine1", null);
                addr.stringType("AddressLine2", null);
                addr.stringType("AddressLine3", null);
                addr.stringType("Country", null);
                addr.stringType("PostCode", "");
                addr.stringType("PostTown", null);
            })
            .minArrayLike("uploadTheNoticeOfDecisionDocs", 1, 1,
                docsUploaded ->
                    docsUploaded.stringType("id", "1")
                        .object("value", v ->
                            v.stringType("description", "some notice of decision description")
                                .object("document", doc ->
                                    doc.stringType("document_url", "http://dm-store-aat.service.core-compute-aat.internal/documents/7f63ca9b-c361-49ab-aa8c-8fbdb6bc2936")
                                        .stringType("document_filename", "some-notice-of-decision-letter.pdf")
                                        .stringType("document_binary_url", "http://dm-store-aat.service.core-compute-aat.internal/documents/7f63ca9b-c361-49ab-aa8c-8fbdb6bc2936"))
                        ) // document object
            ) // minArray
            .object("caseManagementLocation", cml -> {
                cml.stringType("region", "1");
                cml.stringType("baseLocation", "765324");
            })
            .minArrayLike("subscriptions", 1, 1,
                subs ->
                    subs.stringType("id", "1")
                        .object("value", v ->
                            v.stringType("subscriber", "appellant")
                                .stringMatcher("wantsEmail", "Yes|No|YES|NO", "Yes")
                                .stringType("email", "test@example.com")
                                .stringMatcher("wantsSms", "Yes|No|YES|NO", "Yes")
                                .stringType("mobileNumber", "0111111111")
                        )//subscriptions object
            );// minArray
    }

}
