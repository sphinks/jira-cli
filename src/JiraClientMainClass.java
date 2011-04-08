import java.net.URI;
import java.net.URISyntaxException;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;


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
		CommandParcer cp = new CommandParcer();
		Command com = cp.parseCommand(args[0]);
		if (com.equals(new Command(CommandsList.issue,"",""))) {
			final Issue issue = restClient.getIssueClient().getIssue("TST-1", pm);
			System.out.println(issue);
		}

	}

}
