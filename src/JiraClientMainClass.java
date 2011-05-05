import java.net.URISyntaxException;

import com.atlassian.jira.rest.client.domain.BasicProject;


public class JiraClientMainClass {


	/**
	 * Main executable method
	 * 
	 * @param commandLineArguments Commmand-line arguments.
	 */
	public static void main(final String[] commandLineArguments)
	{
		//Start for pipelining
		/*InputStream in;
		 
		if (args.length == 0) {
		  in = new FileInputStream(args[0]);
		}
		else {
		  in = System.in;
		}*/
		JiraCli jiraCli = new JiraCli(commandLineArguments);
		//Test only
		//need new login
		
	}
}
