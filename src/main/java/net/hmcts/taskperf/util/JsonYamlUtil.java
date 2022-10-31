package net.hmcts.taskperf.util;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonYamlUtil
{
	private static final ObjectMapper JSON_MAPPER = makeJsonMapper();
	private static final ObjectMapper YAML_MAPPER = makeYamlMapper();

	public static <T> String writeJsonValue(T object) throws JsonProcessingException
	{
		return JSON_MAPPER.writeValueAsString(object);
	}

	public static <T> T loadJsonResource(String resource, Class<T> clazz) throws IOException
	{
		try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
		{
			T filter = JSON_MAPPER.readValue(input, clazz);
			return filter;
		}
	}

	public static <T> T loadYamlResource(String resource, Class<T> clazz) throws IOException
	{
		try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
		{
			T filter = YAML_MAPPER.readValue(input, clazz);
			return filter;
		}
	}

	/**
	 * Create a JSON {@link ObjectMapper} with a set of sensible defaults.
	 */
	public static ObjectMapper makeJsonMapper()
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return mapper;
	}

	/**
	 * Create a YAML {@link ObjectMapper} with a set of sensible defaults.
	 */
	public static ObjectMapper makeYamlMapper()
	{
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return mapper;
	}

}
