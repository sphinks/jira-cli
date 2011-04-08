import java.util.Iterator;

/**
 * Author: Zerin_IS
 * File: CommandParcer.java
 * Date: 08.04.2011
 */

public class CommandParcer {
	
	public CommandParcer() {
		
	}
	
	public Command parseCommand(String stringToParse) {
		Command currentCommand = null;
		Iterator<Command> commandIterator = CommandsList.commandsList.iterator();
		for (currentCommand = commandIterator.next();commandIterator.hasNext();currentCommand = commandIterator.next()) {
			if (currentCommand.equals(stringToParse)) {
				break;
			}
		}
		return currentCommand;
			
	}
}
