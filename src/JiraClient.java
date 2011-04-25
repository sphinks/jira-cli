import java.net.URI;
import java.net.URISyntaxException;

import org.codehaus.jettison.json.JSONArray;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.BasicProject;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import com.atlassian.jira.rest.client.internal.json.JsonArrayParser;

/**
 * Author: sphinks
 * File: jiraClient.java
 * Date: 17.04.2011
 */

public class JiraClient {
	
	private URI jiraServerUri;
	private JiraRestClient restClient;
	private String login;
	private String password;
	final JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
	final NullProgressMonitor pm = new NullProgressMonitor();
	
	public JiraClient(String URL, String login, String password) throws URISyntaxException{
		
		this.login = login;
		this.password = password;
		restClient = null;
		try{
			jiraServerUri = new URI(URL);
			restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, "sphinks", "654321");
		}catch(URISyntaxException ex){
			throw ex;
		}
	}
	
	/*private void connect() {
		
	}*/
	
	public Issue getIssue(String issueName) {
		return restClient.getIssueClient().getIssue(issueName, pm);
	}
	
	public BasicProject getIssueProject(String issueName) {
		return restClient.getIssueClient().getIssue(issueName, pm).getProject();
	}
	
	

}
