package jiracli.common;
import java.util.HashMap;

import jiracli.actions.CommandAction;
import jiracli.actions.EditIssueAction;
import jiracli.actions.GetIssueAction;
import jiracli.actions.GetProjectAction;
import jiracli.actions.LoginAction;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

/**
 * Author: Zerin_IS
 * File: CommandKind.java
 * Date: 13.04.2011
 */

public class Command {

	public final static Option ISSUE = initIssueOption();
	public final static Option LOGIN = initLoginOption();
	public final static Option HELP = initHelpOption();
	public final static Option GET_PROJECT = initGetProjectOption();
	public final static Option EDIT_ISSUE = initEditIssueOption();
	
	public final static Option GET_SUMMARY = initSummaryOption();
	public final static Option GET_WATCHERS = initWatcherOption();


	public final static HashMap<String, Option> commands = new HashMap<String, Option>();
	public final static HashMap<String, CommandAction> action = new HashMap<String, CommandAction>();

	static {
		commands.put(ISSUE.getOpt(), ISSUE);
		commands.put(LOGIN.getOpt(), LOGIN);
		commands.put(HELP.getOpt(), HELP);
		commands.put(GET_PROJECT.getOpt(), GET_PROJECT);
		commands.put(GET_SUMMARY.getOpt(), GET_SUMMARY);
		commands.put(GET_WATCHERS.getOpt(), GET_WATCHERS);
		commands.put(EDIT_ISSUE.getOpt(), EDIT_ISSUE);
		
		
		action.put(LOGIN.getOpt(), new LoginAction());
		action.put(ISSUE.getOpt(), new GetIssueAction());
		//action.put(HELP.getOpt(), new HelpAction()); Not used at now
		action.put(GET_PROJECT.getOpt(), new GetProjectAction());
		action.put(EDIT_ISSUE.getOpt(), new EditIssueAction());
	}
	
	private static Option initIssueOption() {
		Option option = new Option("i", "issue", true, "Show issue");
		option.setArgs(1);
		option.setArgName("action");
		return option;
	}
	
	private static Option initEditIssueOption() {
		
		Option option  = OptionBuilder.withArgName( "property=value" )
                .hasArgs(2)
                .withValueSeparator()
                .withDescription( "use value for given property to edit issue" )
                .create( "e" );
		
		return option;
		
		/*Option option = new Option("e", "edit", true, "Edit issue");
		option.setArgs(2);
		option.setValueSeparator('=');
		option.setArgName("field");
		return option;*/
	}
	
	private static Option initLoginOption() {
		Option option = new Option("l", "login", true, "Login");
		option.setArgs(3);
		option.setOptionalArg(true);
		option.setArgName("server login password");
		return option;
	}
	
	private static Option initGetProjectOption() {		
		Option option = new Option("getProject", true, "Get project by name");
		option.setArgs(1);
		option.setOptionalArg(false);
		option.setArgName("[name] | [-a]");
		return option;
	}
	
	private static Option initHelpOption() {
		Option option = new Option("h", "help", false, "Show help");
		option.setArgs(0);
		return option;
	}
	
	private static Option initSummaryOption() {
		Option option = new Option("s", false, "Show summary for issues");
		option.setArgs(0);
		return option;
	}
	
	private static Option initWatcherOption() {
		Option option = new Option("w", false, "Show watcher for issues");
		option.setArgs(0);
		return option;
	}
}
