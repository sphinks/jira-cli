import org.apache.commons.cli.Option;

import com.atlassian.jira.rest.client.JiraRestClient;

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
		String str[] = option.getValues();
		return null;
	}

}
