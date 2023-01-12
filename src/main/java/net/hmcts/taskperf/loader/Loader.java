package net.hmcts.taskperf.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import net.hmcts.taskperf.CaseRoleGenerator;
import net.hmcts.taskperf.model.ClientQuery;
import net.hmcts.taskperf.model.Expected;
import net.hmcts.taskperf.model.User;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

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
			int duplicates = user.getCaseRoleDuplicates();
			if (duplicates > 0)
			{
				List<RoleAssignment> roleAssignments = new ArrayList<>();
				for (RoleAssignment roleAssignment : user.getRoleAssignments())
				{
					if (roleAssignment.getRoleType() == RoleType.CASE)
					{
						roleAssignments.addAll(CaseRoleGenerator.multiply(roleAssignment, duplicates));
					}
					else
					{
						roleAssignments.add(roleAssignment);
					}
				}
				user = new User(roleAssignments, 0);
			}
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
