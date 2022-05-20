package net.hmcts.taskperf.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import net.hmcts.taskperf.model.ClientQuery;
import net.hmcts.taskperf.model.Expected;
import net.hmcts.taskperf.model.User;

public class Loader
{
	/**
	 * Since Spring deliberately breaks the standard Java resource loading conventions,
	 * we need to jump through whatever hoops are needed here to find the right resources.
	 */
	public static InputStream getResourceAsStream(String resource) throws IOException
	{
		File root = new File("src/test/resources");
		File file = new File(root, resource);
		return new FileInputStream(file);
	}

	public static User loadUser(String id) throws IOException
	{
		ObjectMapper mapper = makeObjectMapper();
		try (InputStream input = getResourceAsStream("user/user-" + id + ".yml"))
		{
			User user = mapper.readValue(input, User.class);
			return user;
		}
	}

	public static ClientQuery loadClientQuery(String id) throws IOException
	{
		ObjectMapper mapper = makeObjectMapper();
		try (InputStream input = getResourceAsStream("search/search-" + id + ".yml"))
		{
			ClientQuery query = mapper.readValue(input, ClientQuery.class);
			return query;
		}
	}

	public static Expected loadExpected(String id) throws IOException
	{
		ObjectMapper mapper = makeObjectMapper();
		try (InputStream input = getResourceAsStream("expected/expected-" + id + ".yml"))
		{
			Expected expected = mapper.readValue(input, Expected.class);
			return expected;
		}
	}

	/**
	 * Create an {@link ObjectMapper} with a set of sensible defaults.
	 */
	public static ObjectMapper makeObjectMapper()
	{
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return mapper;
	}
}
