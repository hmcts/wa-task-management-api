package net.hmcts.taskperf.model;

import java.util.Map;

import lombok.Value;

@Value
public class Task
{
	private String taskId;
	private Map<String, Object> attributes;
}
