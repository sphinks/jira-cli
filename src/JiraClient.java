/**
 * Author: sphinks
 * File: jiraClient.java
 * Date: 17.04.2011
 */

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.cli.Option;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;

public class JiraClient {
	
	private URI jiraServerUri;
	private JiraRestClient restClient;
	private String login;
	private String password;
	final JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
	
	public JiraClient(String URL, String login, String password) throws URISyntaxException{
		
		this.login = login;
		this.password = password;
		restClient = null;
		try{
			jiraServerUri = new URI(URL);
			restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, login, password);
		}catch(URISyntaxException ex){
			throw ex;
		}
	}
	
	public String performCommand(Option opt) {
		CommandAction commandAction = Command.action.get(opt.getOpt());
		return commandAction.action(restClient);
	}
	
	/*public Issue getIssue(String issueName) {
		return restClient.getIssueClient().getIssue(issueName, pm);
	}
	
	public BasicProject getIssueProject(String issueName) {
		return restClient.getIssueClient().getIssue(issueName, pm).getProject();
	}*/
	
	

}
