package jiracli.actions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jiracli.common.Command;

import org.apache.commons.cli.Option;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.domain.BasicWatchers;
import com.atlassian.jira.rest.client.domain.Issue;

/**
 * @author Zerin_IS
 * @date 05.05.2011
 *
 */
public class GetIssueAction extends CommandAction {
	
	final static HashMap<String, Option> additionalOptions = new HashMap<String, Option>();
	
	static{
		additionalOptions.put(Command.GET_SUMMARY.getOpt(), Command.GET_SUMMARY);
	}

	/* (non-Javadoc)
	 * @see CommandAction#action(org.apache.commons.cli.Option, com.atlassian.jira.rest.client.JiraRestClient)
	 */
	@Override
	public String action(Option[] options, JiraRestClient restClient) {
		Option option = options[0];
		String[] arguments = option.getValues();
		List<Option> optList = Arrays.asList(options);
		Issue issue;
		String result = "";
		
		if (arguments.length > 0) {
			issue = restClient.getIssueClient().getIssue(arguments[0], pm);
			result = "Issue: " + issue.getKey() + " (" + issue.getSelf().toString() + ")\r\n";
			if (optList.contains(Command.GET_SUMMARY)) {
				result += "Summary: " + issue.getSummary() + "\r\n";
				result += "Transaction URI: " + issue.getTransitionsUri() + "\r\n";
			}
			if (optList.contains(Command.GET_WATCHERS)) {
				BasicWatchers bw = issue.getWatchers();
				result += "Watchers: " + bw.getNumWatchers() + "\r\n" + "Watch: " + bw.isWatching() + "\r\n";
			}
			return result;
		}else{
			result = "Error: specify name of looking issue.";
			return result;
		}
	}

}
