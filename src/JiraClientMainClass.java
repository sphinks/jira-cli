import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;

import org.apache.commons.cli.*;


public class JiraClientMainClass {


	/**
	 * Main executable method
	 * 
	 * @param commandLineArguments Commmand-line arguments.
	 */
	public static void main(final String[] commandLineArguments)
	{
		JiraCli jiraCli = new JiraCli(commandLineArguments);
		//Test only
		try{
			JiraClient jc = new JiraClient("http://sandbox.onjira.com", "sphinks", "654321");
			//System.out.println(jc.getIssue("TST-1"));
			System.out.println(jc.getIssueProject("TST-1"));
		}catch(URISyntaxException ex){
			System.err.println("Incorrect URI: " + ex.toString());
		}
	}
}
