/**
 * Author: sphinks
 * File: Grammar.java
 * Date: 11.04.2011
 */

import org.apache.commons.cli.CommandLine;

public abstract class Grammar {
	
	abstract void addRule(Rule newRule);
	abstract ParserResult parser(CommandLine commandLine);

}
