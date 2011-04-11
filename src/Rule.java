/**
 * Author: Zerin_IS
 * File: Rule.java
 * Date: 11.04.2011
 */


public class Rule {

	private Object left;
	private Sequence right;

	public Rule(Object left, Sequence right) {
		this.left = left;
		this.right = right;
	}

	public Rule(Object left, Object[] right) {
		this.left = left;
		this.right = new Sequence(right);
	}

	public Object left() {
		return left;
	}

	public Sequence right() {
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
		return left + " : " + right;
	}
}
