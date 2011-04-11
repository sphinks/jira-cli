/**
 * Author: Zerin_IS
 * File: NumberPattern.java
 * Date: 11.04.2011
 */

package scanner;

class NumberPattern extends Pattern {

	MatchResult match(char[] in, int start) {
		int i = start;
		boolean hasDot = false;
		for (; i < in.length && CharUtils.isDigit(in[i]) && in[i] != '{' && in[i] != '=' && in[i] != ':' && in[i] != ';'; ++i) { }
		if (i < in.length && in[i] == '.' && i != start) {
			++i;
			hasDot = true;
			for (; i < in.length && CharUtils.isDigit(in[i]); ++i) { }
		}
		if (i > start) {
			Token token;
			String s = new String(in, start, i - start);
			if (hasDot) {
				token = new RealToken(Double.parseDouble(s));
			} else {
				token = new IntegerToken(Integer.parseInt(s));
			}
			return new TokenMatchResult(i - start, token);
		} else {
			return FailureMatchResult.INSTANCE;
		}
	}
}