import org.apache.commons.cli.Option;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.TimeTracking;

/**
 * @author Zerin_IS
 * @date 05.05.2011
 *
 */
public class GetIssueAction extends CommandAction {

	/* (non-Javadoc)
	 * @see CommandAction#action(org.apache.commons.cli.Option, com.atlassian.jira.rest.client.JiraRestClient)
	 */
	@Override
	public String action(Option option, JiraRestClient restClient) {
		String[] arguments = option.getValues();
		Issue issue;
		String result = "";
		
		if (arguments.length > 0) {
			issue = restClient.getIssueClient().getIssue(arguments[0], pm);
			result = "Issue: " + issue.getKey() + " (" + issue.getSelf().toString() + ")\r\n" + "Summary: " + issue.getSummary() + "\r\n";
			
			result += "\r\n" + "Time for issue: " + issue.getTimeTracking().getOriginalEstimateMinutes() + " min";
			result += "\r\n" + "Time for left issue: " + issue.getTimeTracking().getRemainingEstimateMinutes() + " min";
			
			return result;
		}else{
			result = "Error: specify name of looking issue.";
			return result;
		}
	}

}
