import java.util.HashMap;

import org.apache.commons.cli.Option;

/**
 * Author: Zerin_IS
 * File: CommandKind.java
 * Date: 13.04.2011
 */

public class Command {

	final static Option ISSUE = initIssueOption();
	final static Option LOGIN = initLoginOption();
	final static Option HELP = initHelpOption();
	final static Option GET_PROJECT = initGetProjectOption();


	final static HashMap<String, Option> commands = new HashMap<String, Option>();
	final static HashMap<String, CommandAction> action = new HashMap<String, CommandAction>();

	static {
		commands.put(ISSUE.getOpt(), ISSUE);
		commands.put(LOGIN.getOpt(), LOGIN);
		commands.put(HELP.getOpt(), HELP);
		commands.put(GET_PROJECT.getOpt(), GET_PROJECT);
		
		action.put(LOGIN.getOpt(), new LoginAction());
		action.put(ISSUE.getOpt(), new GetIssueAction());
		//action.put(HELP.getOpt(), new HelpAction()); Not used at now
		action.put(GET_PROJECT.getOpt(), new GetProjectAction());
	}
	
	private static Option initIssueOption() {
		Option option = new Option("issue", true, "Show issue");
		option.setArgs(2);
		option.setArgName("action");
		return option;
	}
	
	private static Option initLoginOption() {
		Option option = new Option("login", true, "Login");
		option.setArgs(3);
		option.setOptionalArg(true);
		option.setArgName("<server> <login> <password>");
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
		Option option = new Option("help", true, "Show help");
		option.setArgs(0);
		return option;
	}
}
