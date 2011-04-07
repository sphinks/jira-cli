import java.net.URI;
import java.net.URISyntaxException;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;


public class JiraClientMainClass {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		final JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
		try{
			final URI jiraServerUri = new URI("http://sandbox.onjira.com:8089/jira");
			final JiraRestClient restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, "sphinks", "654321");
			final NullProgressMonitor pm = new NullProgressMonitor();
			final Issue issue = restClient.getIssueClient().getIssue("TST-1", pm);

			System.out.println(issue);
		}catch(URISyntaxException ex){
			
		}
		

	}

}
