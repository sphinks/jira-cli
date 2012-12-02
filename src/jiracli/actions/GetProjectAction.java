package jiracli.actions;

import java.util.Iterator;

import org.apache.commons.cli.Option;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.RestClientException;
import com.atlassian.jira.rest.client.domain.BasicProject;

/**
 * @author Zerin_IS
 * @date 05.05.2011
 *
 */
public class GetProjectAction extends CommandAction {

	/* (non-Javadoc)
	 * @see CommandAction#action(org.apache.commons.cli.Option, com.atlassian.jira.rest.client.JiraRestClient)
	 */
	@Override
	public String action(Option[] options, JiraRestClient restClient) {
		Option option = options[0];
		String[] arguments = option.getValues();
		BasicProject bp;
		String result = "";
		
		if (arguments[0].equals("-a")) {
			Iterable<BasicProject> projects = restClient.getProjectClient().getAllProjects(pm);
			for (Iterator<BasicProject> iter = projects.iterator(); iter.hasNext();) {
				bp = iter.next();
				result += bp.getKey() + " (" + bp.getSelf().toString() + ")\r\n"; 
			}
			return result;
		}else{
			try{
				bp = restClient.getProjectClient().getProject(arguments[0], pm);
				result = bp.getKey() + " (" + bp.getSelf().toString() + ")"; 
			}catch(RestClientException rce){
				result = "Error: " + rce.getMessage();
			}
			return result;
		}
	}

}
