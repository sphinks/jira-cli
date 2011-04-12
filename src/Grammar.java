/**
 * Author: sphinks
 * File: Grammar.java
 * Date: 11.04.2011
 */

import java.util.List;
import scanner.*;

public abstract class Grammar {
	
	abstract void addRule(Rule newRule);
	abstract List<Command> parser(Sequence tokenArray);

}
