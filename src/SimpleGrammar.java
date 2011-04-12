import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import scanner.Token;

/**
 * Author: sphinks
 * File: SimpleGrammar.java
 * Date: 11.04.2011
 */

public class SimpleGrammar extends Grammar {

	private LinkedList<Rule> grammarRules;
	
	
	
	public SimpleGrammar() {
		grammarRules = new LinkedList();
	}

	@Override
	public void addRule(Rule newRule) {
		grammarRules.add(newRule);
	}

	@Override
	public LinkedList<Command> parser(Sequence tokenArray) {
		Iterator ruleIterator;
		Sequence longest;
		Rule r;
		for (int i = 0; i < tokenArray.length(); i++) {
			ruleIterator = grammarRules.iterator();
			while (ruleIterator.hasNext()) {
				r = (Rule)ruleIterator.next();
				if (r.right().length() < tokenArray.length() - i) {
					if (canReduce(r, tokenArray.subSequence(i, r.right().length()), r.right().length())) {
						//System.
					}
				}
			}
			
		}
		return null;
	}
	
	
	private void reduce(Rule r, int start, int lenght) {
		
		
	}
	
	/*private boolean tryToReduce(Rule r, Token[] tokenArray) {
		for (int i = 0; i < tokenArray.length; i++) {
			
		}
	}*/
	
	private boolean canReduce(Rule r, Sequence stack, int current) {
		if (current > 0) {
			if (r.right().at(current).equals(stack.at(current))) {
				return canReduce(r, stack, current-1);
			}else{
				return false;
			}
		}else{
			return true;
		}
	}

}
