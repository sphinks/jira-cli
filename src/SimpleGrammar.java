import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

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
	public ParserResult parser(CommandLine commandLine) {
		
		
		Option baseOpt = getBaseOption(commandLine);
		

		Iterator ruleIterator;
		Rule r = Rule.getUndefineRuleInstance();
		ParserResult parserResult;
		Rule tmpRule;
		ruleIterator = grammarRules.iterator();
		while (ruleIterator.hasNext()) {
			tmpRule = (Rule)ruleIterator.next();
			if ((tmpRule.left().equals(baseOpt)) && (tmpRule.right().length == commandLine.getOptions().length-1)) {
				if (canReduce(tmpRule, commandLine, tmpRule.right().length)) {
					return new SuccessfullParserResult(tmpRule);
				}
			}
		}
		return new FailedParserResult(r);
	}
	
	private boolean canReduce(Rule r, CommandLine stack, int current) {
		if (current > 0) {
			if (stack.hasOption(((Option)r.right()[current-1]).getOpt())){
				return canReduce(r, stack, current-1);
			}else{
				return false;
			}
		}else{
			return true;
		}
	}
	
	private Option getBaseOption(CommandLine commandLine) {
		Iterator<String> iter = Command.baseCommand.iterator();
		Option baseOpt = null;
		String tmpOpt;
		while (iter.hasNext()) {
			tmpOpt = (String)iter.next();
			if ( commandLine.hasOption(tmpOpt)) {
				baseOpt = Command.commands.get(tmpOpt);
				break;
			}
		}
		//TODO check that there is at least one base command and that there is no more that one base command
		return baseOpt;
	}

}
