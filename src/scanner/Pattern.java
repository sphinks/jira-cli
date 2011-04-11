/**
 * Author: Zerin_IS
 * File: Pattern.java
 * Date: 11.04.2011
 */

package scanner;

abstract class Pattern {

	abstract MatchResult match(char[] in, int start);
}