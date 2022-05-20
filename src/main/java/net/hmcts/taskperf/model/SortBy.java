package net.hmcts.taskperf.model;

import lombok.Value;

@Value
public class SortBy
{
	private Column column;
	private Direction direction;

	public static enum Column
	{
	    DUE_DATE_TIME("due_date_time"),
	    CASE_NAME("case_name"),
	    CASE_CATEGORY("case_category"),
	    LOCATION("location"),
	    TASK_NAME("task_name"),
	    CREATED("created"),
		MAJOR_PRIORITY("major_priority"),
		PRIORITY_DATE_TIME("priority_date_time"),
		MINOR_PRIORITY("minor_priority");
	
	    private final String value;
	
		Column(String value) {
	        this.value = value;
	    }
	
	    public String value() {
	        return value;
	    }
	}

	public static enum Direction
	{
	    ASC("asc"),
	    DESC("desc");
	    private final String value;

	    Direction(String value) {
	        this.value = value;
	    }

	    public String value() {
	        return value;
	    }
	}
}
