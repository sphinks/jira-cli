import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import org.apache.commons.cli.Option;
/**
 * @author Zerin_IS
 *
 */

public abstract class CommandAction {
	
	protected final NullProgressMonitor pm = new NullProgressMonitor();
	
	public abstract String action(Option[] options, JiraRestClient restClient);

}
