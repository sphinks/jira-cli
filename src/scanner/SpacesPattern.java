/**
 * Author: Zerin_IS
 * File: SpacesPattern.java
 * Date: 11.04.2011
 */

package scanner;

class SpacesPattern extends Pattern {

	MatchResult match(char[] in, int start) {
		int i = start;
		for (; i < in.length && in[i] == ' '; ++i) { }
		if (i > start) {
			return new IgnorableMatchResult(i - start);
		} else {
			return FailureMatchResult.INSTANCE;
		}
	}
}