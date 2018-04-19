package terraform_to_ansible;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import terraform_to_ansible.TerraformToAnsible;
import static terraform_to_ansible.TerraformToAnsible.*;

/**
 * 
 */
public class TestTerraformToAnsible
{
	/**
	 * Read in terraform.tfstate file and check map returned contains correct IP and host name.
	 */
	@Test
	public void parseExampleTerraformState() throws Throwable
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name());
		
		Map<String, List<Map<String, String>>> inv = new TerraformToAnsible(FileSystems.getDefault().getPath("src/test/resources/terraform.tfstate").toAbsolutePath().toFile().toString(),
							   										  		ps, "# Prepended content, e.g. ansible variables, etc.", null).run();
		
		assertNotNull(inv);
		assertEquals(1, inv.keySet().size());
		assertEquals(1, inv.values().size());
		assertEquals("webserver", inv.keySet().iterator().next());
		assertEquals("172.31.0.0", inv.get("webserver").get(0).get(PRIVATE_IPV4));
		assertEquals("00.00.111.22", inv.get("webserver").get(0).get(IPV4));
		assertEquals("aws_instance.my_blog", inv.get("webserver").get(0).get(TERRAFORM_INSTANCE_NAME));
		assertEquals("master", inv.get("webserver").get(0).get("database_replica_type"));
		
		String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		ps.close();
		baos.close();
		
		assertTrue(content.contains("\n00.00.111.22"));
		assertTrue(content.contains("database_replica_type=master"));
	}
}
