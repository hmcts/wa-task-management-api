package uk.gov.hmcts.reform.wataskmanagementapi.services;

public interface FeatureToggler {

    boolean getValue(String key, Boolean defaultValue);

}
