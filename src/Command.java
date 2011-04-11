import scanner.Token;

/**
 * Author: sphinks
 * File: Command.java
 * Date: 11.04.2011
 */

public class Command {

	private Token command;
	private Token[] modifiers;
	private Token[] parametrs;
	
	public Command (Token command, Token[] modifiers, Token[] parametrs) {
		this.command = command;
		this.modifiers = modifiers;
		this.parametrs = parametrs;
	}

	public Token getCommand() {
		return command;
	}

	public Token[] getModifiers() {
		return modifiers;
	}

	public Token[] getParametrs() {
		return parametrs;
	}
	
	public String toString() {
		return command + " -" + modifiers.toString() + " " + parametrs.toString();
	}
	
}
