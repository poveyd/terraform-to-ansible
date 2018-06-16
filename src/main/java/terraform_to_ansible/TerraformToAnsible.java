package terraform_to_ansible;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.PropertyConfigurator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.json.simple.parser.JSONParser;

/**
 * <p>This code is work-in-progress. Just a convenient way of picking out the host from the terraform.tfstate file.</p>
 * 
 * <p>Tested against Terraform 0.11.3 config files only. Only tested for AWS provider.</p>
 * 
 * Disclaimer:
 * This software is not thoroughly tested. Author(s) accept(s) no responsibility nor liability for any negative effects whatsoever arising from the use of this software.
 *
 */
public final class TerraformToAnsible
{
	private static final String COMMAND = "java -jar terraform_to_ansible.jar";
	public static final String IPV4 = "ipv4";
	public static final String TERRAFORM_INSTANCE_NAME = "terraform_instance";
	public static final String PRIVATE_IPV4 = "private_ipv4";
	
	private final File terraformState;
	private final String prepend;
	private final PrintStream ansibleInventory;
	private final List<String> ansibleTagList;
	
	public static void main(String[] args) throws InterruptedException, Exception
	{
		Options options = new Options().addOption("tf", "terraform-file", true, "location of terraform.tfstate file")
									   .addOption("a", "ansible-inventory", true, "name of Ansible inventory file to be created")
									   .addOption("t", "ansible-tags", true, "Pick off this set of tags from Terraform EC2 instance and place them at the end of each server entry in the Ansible inventory file (e.g. specify \"-t ansible_user\" in parameters then with a tag of Ansible_user=root in the Terraform EC2 config, you will get 1.2.3.4 ansible_user=root in the Ansible inventory file. If not present then any tags starting with \"Ansible-\" will be appended to the end of the server line in the Ansible inventory file (minus the \"Ansible-\" bit). Please do not use spaces in your tags.")
									   .addOption("p", "prepend", true, "prepend this text to the start of the Ansible inventory file")
									   .addOption("h", "help", false, "Print out these help options");
		
		CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try
        {
        	cmd = parser.parse(options, args);
        }
        catch( ParseException pe )
        {
        	System.err.println("Caught illegal set of arguments: " + args + ". " + pe.getMessage());
			printUseage(options);
        	System.exit(1);
        }
        
        if( cmd.hasOption("h") )
        {
			printUseage(options);
			System.exit(0);
        }
        
        if( null != Thread.currentThread().getContextClassLoader().getResource("log4j_config.txt") )
		{
			PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResource("log4j_config.txt"));
		}
        
        String terraformStateFile = cmd.hasOption("tf") && !cmd.getOptionValue("tf").trim().isEmpty() ? cmd.getOptionValue("tf") : null;
        String prepend = cmd.hasOption("p") && !cmd.getOptionValue("p").trim().isEmpty() ? cmd.getOptionValue("p") : null;
        String ansibleTags = cmd.hasOption("t") && !cmd.getOptionValue("t").trim().isEmpty() ? cmd.getOptionValue("t") : null;
		PrintStream ansibleInventoryFile = cmd.hasOption("a") && !cmd.getOptionValue("a").trim().isEmpty() ? new PrintStream(new FileOutputStream(cmd.getOptionValue("a"))) : System.out;
		
		if( (null == terraformStateFile) )
		{
			System.err.println("Reading in terraform.tfstate file from current directory."); // Write to STDERR to prevent problems with redirecting output to file from STDOUT.
			terraformStateFile = "./terraform.tfstate";
		}
		
		List<String> ansibleTagList;
		if( (null != ansibleTags) && !ansibleTags.trim().isEmpty() )
		{
			ansibleTagList = new ArrayList<>(Arrays.asList(ansibleTags.replaceAll("\\w", "").split(",")));
		}
		else
		{
			ansibleTagList = null;
		}
		
		new TerraformToAnsible(terraformStateFile, ansibleInventoryFile, prepend, ansibleTagList).run();
	}
	
	public TerraformToAnsible(String from, PrintStream ansibleInventoryFile, String prepend, List<String> ansibleTagList)
	{
		terraformState = new File(from);
		if( !terraformState.exists() || !terraformState.isFile() )
		{
			throw new IllegalArgumentException("The location of the terraform.tfstate file (tf argument) needs to be a valid file, and be readable.");
		}
		
		if( null != ansibleInventoryFile )
			ansibleInventory = ansibleInventoryFile;
		else
			ansibleInventory = System.out;
		this.prepend = prepend;
		this.ansibleTagList = ansibleTagList;
	}
	
	/**
	 * @param options
	 */
	private static void printUseage(Options options)
	{
		new HelpFormatter().printHelp(COMMAND + " - create an Ansible inventory file from a terraform.tfstate file.", options);
	}
	
	public Map<String, List<Map<String, String>>> run()
	{
		// Key is Ansible_host (case-insensitive), value is a map from Ansible variable (including one have created here, IPV4) to variable value.
		Map<String, List<Map<String, String>>> inv = new HashMap<>();
		
		JSONParser parser = new JSONParser();
		
		JSONObject a = null;
		try
		{
			a = (JSONObject) parser.parse(new FileReader(this.terraformState));
		}
		catch( IOException | org.json.simple.parser.ParseException e)
		{
			throw new RuntimeException(e);
		}
		
		JSONArray modules = (JSONArray) a.get("modules");
		
		for (Object o : modules)
		{
			JSONObject module = (JSONObject) o;
			
			if( module.containsKey("resources") )
			{
				JSONObject resources = (JSONObject) module.get("resources");
				for( Object key : resources.keySet() )
				{
					if( key.toString().startsWith("aws_instance") )
					{
						JSONObject instanceKeys = ((JSONObject) ((JSONObject) ((JSONObject) resources.get(key)).get("primary")).get("attributes"));
						Map<String, Object> keyMapping = (Map<String, Object>) instanceKeys.keySet().stream().collect(Collectors.toMap(k -> k.toString().toLowerCase(), k -> k));
						if( keyMapping.containsKey("tags.ansible_host") )
						{
							String host = ((JSONObject) instanceKeys).get(keyMapping.get("tags.ansible_host")).toString();
							if( !inv.containsKey(host) )
								inv.put(host, new ArrayList<>());
							Map<String, String> thisHost = new HashMap<>();
							thisHost.put(IPV4, instanceKeys.get("public_ip").toString());
							thisHost.put(TERRAFORM_INSTANCE_NAME, key.toString());
							thisHost.put(PRIVATE_IPV4, instanceKeys.get("private_ip").toString());
							if( null != ansibleTagList )
							{
								for( String tag : ansibleTagList )
								{
									if( instanceKeys.containsKey(tag) )
									{
										thisHost.put(tag, instanceKeys.get(tag).toString());
									}
								}
							}
							else
							{
								for( Object tag : instanceKeys.keySet() )
								{
									if( tag.toString().startsWith("tags.Ansible-") )
									{
										thisHost.put(tag.toString().substring("tags.Ansible-".length()), instanceKeys.get(tag).toString());
									}
								}
							}
							inv.get(host).add(thisHost);
						}
					}
				}
			}
			
		}
		
		this.ansibleInventory.print("# Auto-generated by " + this.getClass().getSimpleName() + ".\n");
		
		if( null != prepend )
			this.ansibleInventory.print("\n" + prepend.replace("\\n", "\n") + "\n");
		
		for( String ansibleHost : inv.keySet().stream().sorted().collect(Collectors.toList()) )
		{
			this.ansibleInventory.print("\n[" + ansibleHost + "]\n");
			
			for( Map<String, String> keys : inv.get(ansibleHost) )
			{
				this.ansibleInventory.print("# Terraform instance name: " + keys.get(TERRAFORM_INSTANCE_NAME) + "\n");
				this.ansibleInventory.print(keys.get(IPV4));
				if( keys.containsKey(PRIVATE_IPV4) )
					this.ansibleInventory.print(" private_ip=" + keys.get(PRIVATE_IPV4));
				
				for( String key : keys.keySet() )
					if( !key.equals(IPV4) && !key.equals(PRIVATE_IPV4) && !key.equals(TERRAFORM_INSTANCE_NAME) )
						this.ansibleInventory.print(" " + key + "=" + keys.get(key));
				
				this.ansibleInventory.print("\n");
			}
		}
		
		return inv;
	}
}
