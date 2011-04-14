import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.Option;

/**
 * Author: Zerin_IS
 * File: CommandKind.java
 * Date: 13.04.2011
 */

public class Command {

	final static Option ISSUE = initIssueOption();
	final static Option LOGIN = initLoginOption();


	final static HashMap<String, Option> commands = new HashMap<String, Option>();
	final static List<String> baseCommand = new LinkedList<String>();

	static {
		commands.put(ISSUE.getOpt(), ISSUE);
		commands.put(ISSUE.getOpt(), LOGIN);
		
		baseCommand.add(ISSUE.getOpt());
	}
	
	private static Option initIssueOption() {
		Option option = new Option("issue", true, "Show issue");
		option.setArgs(2);
		option.setArgName("action");
		return option;
	}
	
	private static Option initLoginOption() {
		Option option = new Option("login", true, "Login");
		option.setArgs(2);
		option.setArgName("action");
		return option;
	}
}
