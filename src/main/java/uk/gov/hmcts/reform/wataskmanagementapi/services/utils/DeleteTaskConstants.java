package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

public final class DeleteTaskConstants {

    public static final String CASE_ID_REGEX = "^\\d{16}$";
    public static final String CASEID_DELETE_EXCEPTION_MESSAGE = "Unable to verify case-id path parameter pattern: ";

    private DeleteTaskConstants() {
    }
}
