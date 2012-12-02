package jiracli.actions;

import org.apache.commons.cli.Option;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.input.TransitionInput;

public class EditIssueAction extends CommandAction {
	
	@Override
	public String action(Option[] options, JiraRestClient restClient) {
		
		return "Perform edit on field " + options[0].getValue(0) + " with value " + options[0].getValue(1);
	}

}
