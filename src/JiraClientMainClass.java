import java.net.URI;
import java.net.URISyntaxException;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;

import scanner.*;


public class JiraClientMainClass {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		URI jiraServerUri = null;
		JiraRestClient restClient = null;
		final NullProgressMonitor pm = new NullProgressMonitor();
		
		final JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
		try{
			jiraServerUri = new URI("http://sandbox.onjira.com");
			restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, "sphinks", "654321");
			

			
		}catch(URISyntaxException ex){
			System.out.println("Can`t establish connection with " + jiraServerUri.toString());
		}
		
		Scanner scanner = new Scanner();
		char[] in;
		String tmpStr = "";
		for (int i = 0; i < args.length; i++) {
			tmpStr += args[i] + ' ';
		}
		in = tmpStr.toCharArray(); 
		Token[] tokenArray = scanner.scan(in);
		System.out.println("//////////");
	    for (int i = 0; i < tokenArray.length; i++){
	    	System.out.println(tokenArray[i].getKind());
	    }
		/*CommandParcer cp = new CommandParcer();
		Command com = cp.parseCommand(args[0]);
		if (com.equals(new Command(CommandsList.issue,"",""))) {
			final Issue issue = restClient.getIssueClient().getIssue("TST-1", pm);
			System.out.println(issue);
		}*/

	}

}
