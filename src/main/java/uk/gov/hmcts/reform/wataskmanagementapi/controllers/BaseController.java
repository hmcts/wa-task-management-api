package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class BaseController {

    protected static final String OK = "OK";
    protected static final String NO_CONTENT = "No content";
    protected static final String UNAUTHORIZED = "Unauthorized";
    protected static final String BAD_REQUEST = "Bad Request";
    protected static final String FORBIDDEN = "Forbidden";
    protected static final String NOT_FOUND = "Not found";
    protected static final String UNSUPPORTED_MEDIA_TYPE = "Unsupported Media Type";
    protected static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    protected static final String TASK_ID = "task-id";

    protected BaseController() {
        //Only really want it called from a subclass
    }
}
