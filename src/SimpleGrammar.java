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
		grammarRules = new LinkedList<Rule>();
	}

	@Override
	public void addRule(Rule newRule) {
		grammarRules.add(newRule);
	}

	@Override
	public Rule parser(Object[] commandArray) {
		Iterator ruleIterator;
		Rule r = Rule.getUndefineRuleInstance();
		Rule tmpRule;
		ruleIterator = grammarRules.iterator();
		while (ruleIterator.hasNext()) {
			tmpRule = (Rule)ruleIterator.next();
			if (tmpRule.right().length == commandArray.length) {
				if (canReduce(tmpRule, commandArray, tmpRule.right().length)) {
					r = tmpRule;
					break;
				}
			}
		}
		return r;
	}
	
	
	private void reduce(Rule r, int start, int lenght) {
		
		
	}
	
	/*private boolean tryToReduce(Rule r, Token[] tokenArray) {
		for (int i = 0; i < tokenArray.length; i++) {
			
		}
	}*/
	
	private boolean canReduce(Rule r, Object[] stack, int current) {
		if (current > 0) {
			if (r.right()[current-1].equals(stack[current-1])) {
				return canReduce(r, stack, current-1);
			}else{
				return false;
			}
		}else{
			return true;
		}
	}

}
