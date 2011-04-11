/**
 * Author: Zerin_IS
 * File: StringPattern.java
 * Date: 11.04.2011
 */

package scanner;

class StringPattern extends Pattern{

	MatchResult match(char[] in, int start) {
		int i = start;
		if (in[start] == '"') {
			for (; i < in.length && in[i+1] != '"'; ++i) { }
			i++;
		}
		if (i > start) {
			String s = new String(in, start, i - start);
			Token token = new StringToken(s);
			return new TokenMatchResult(i - start, token);
		} else {
			return FailureMatchResult.INSTANCE;
		}
	}
}