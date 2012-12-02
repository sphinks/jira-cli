package jiracli.actions;

import org.apache.commons.cli.Option;

import com.atlassian.jira.rest.client.JiraRestClient;

/**
 * @author Zerin_IS
 * @date 04.05.2011
 *
 */
public class LoginAction extends CommandAction {


	/* (non-Javadoc)
	 * @see CommandAction#action()
	 */
	@Override
	public String action(Option[] options, JiraRestClient restClient) {
		return restClient.getSessionClient().getCurrentSession(pm).toString();
	}

}
