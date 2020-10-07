package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import java.util.List;

public class SearchParameters {

    private List<String> jurisdiction;
    private List<String> user;
    private List<String> location;
    private List<String> state;
    private String ccdId;
    private String eventId;
    private String preEventState;
    private String postEventState;

    public SearchParameters() {
        //Default constructor for deserialization
        super();
    }

    public SearchParameters(List<String> jurisdiction, List<String> user, List<String> location, List<String> state, String ccdId, String eventId, String preEventState, String postEventState) {
        this.jurisdiction = jurisdiction;
        this.user = user;
        this.location = location;
        this.state = state;
        this.ccdId = ccdId;
        this.eventId = eventId;
        this.preEventState = preEventState;
        this.postEventState = postEventState;
    }

    public List<String> getJurisdiction() {
        return jurisdiction;
    }

    public List<String> getUser() {
        return user;
    }

    public List<String> getLocation() {
        return location;
    }

    public List<String> getState() {
        return state;
    }

    public String getCcdId() {
        return ccdId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getPreEventState() {
        return preEventState;
    }

    public String getPostEventState() {
        return postEventState;
    }
}
