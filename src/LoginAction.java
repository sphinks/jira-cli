import org.apache.commons.cli.Option;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;

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
	public String action(Option option, JiraRestClient restClient) {
		return restClient.getSessionClient().getCurrentSession(pm).toString();
	}

}
