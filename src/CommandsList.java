import java.util.HashSet;

/**
 * Author: Zerin_IS
 * File: Command.java
 * Date: 08.04.2011
 */

public class CommandsList {
	
	final static String project = "project";
	final static String issue = "issue";
	final static String help = "help";
	final static String unknown = "unknown";
	
	final static HashSet<Command> commandsList = new HashSet<Command>();
	
	static{
		commandsList.add(new Command(project,"",""));
		commandsList.add(new Command(issue,"",""));
		commandsList.add(new Command(help,"",""));
		commandsList.add(new Command(unknown,"",""));
	}
	
}
