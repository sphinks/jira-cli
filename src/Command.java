/**
 * Author: Zerin_IS
 * File: Command.java
 * Date: 08.04.2011
 */

public class Command {
	
	private String command;
	private String commandHelp;
	private String parametrs;
	
	public Command(String command, String commandHelp, String parametrs)
	{
		this.command = command;
		this.commandHelp = commandHelp;
		this.parametrs = parametrs;
	}
	
	public Command()
	{
		this.command = CommandsList.unknown;
		this.commandHelp = "";
		this.parametrs = "";
	}
	
	public String getParametrs() {
		return parametrs;
	}

	public String getCommand() {
		return command;
	}

	public String getCommandHelp() {
		return commandHelp;
	}
	
	@Override
	public boolean equals(Object o) {
		String commandToCompare = "";
		if (o instanceof Command) {
			commandToCompare = ((Command)o).getCommand();
		}
		if (o instanceof String) {
			commandToCompare = ((String)o);
		}
		return commandToCompare.equals(this.getCommand());
	}

}
