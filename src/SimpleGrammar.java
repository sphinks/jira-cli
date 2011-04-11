import java.util.HashMap;
import java.util.List;

import scanner.Token;

/**
 * Author: sphinks
 * File: SimpleGrammar.java
 * Date: 11.04.2011
 */

public class SimpleGrammar extends Grammar {

	private List<Rule> grammarRules;
	
	
	
	public SimpleGrammar() {
		grammarRules = new List();
	}

	@Override
	public void addRule(Rule newRule) {


	}

	@Override
	public List<Command> parser(Token[] tokenArray) {
		// TODO Auto-generated method stub
		return null;
	}

}
