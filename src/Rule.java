import org.apache.commons.cli.Option;

/**
 * Author: Zerin_IS
 * File: Rule.java
 * Date: 11.04.2011
 */


public class Rule {
	
	private static final Rule UNDEFINED_RULE = new Rule("undefined", null);

	private Object left;
	private Object[] right;

	public Rule(Object left, Object[] right) {
		this.left = left;
		this.right = right;
	}

	/*public Rule(Object left, Object[] right) {
		this.left = left;
		this.right = new Sequence(right);
	}*/

	public Object left() {
		return left;
	}

	public Object[] right() {
		return right;
	}

	public boolean equals(Object o) {
		return o instanceof Rule ? equals((Rule) o) : false;
	}

	public boolean equals(Rule o) {
		return left.equals(o.left) && right.equals(o.right);
	}

	public int hashCode() {
		return left.hashCode() ^ right.hashCode();
	}

	public String toString() {
		if (left instanceof Option) {
			String ruleInString = ((Option)left).getOpt() + ':';
			
			for (int i = 0; i < right.length; i++) {
				if (right[i] instanceof Option) {
					ruleInString += ((Option)right[i]).getOpt();
				}else{
					ruleInString += right[i].toString();
				}
				if (i == right.length-1) {
					ruleInString += ';';
				}else{
					ruleInString += ':';
				}
			}
			return ruleInString;
		}else{
			return left + " : " + right;
		}
	}
	
	public static Rule getUndefineRuleInstance() {
		return UNDEFINED_RULE;
	}
}
