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
		try{
			JiraClient jc = new JiraClient("http://sandbox.onjira.com", "sphinks", "654321");
			//System.out.println(jc.getIssueProject("TST-1"));
			BasicProject bp = jc.getIssueProject("TST-1");
			System.out.println(bp.getKey());
			System.out.println(bp.getSelf());

		}catch(URISyntaxException ex){
			System.err.println("Incorrect URI: " + ex.toString());
		}
	}
}
