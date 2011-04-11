/**
 * Author: Zerin_IS
 * File: CharacterPattern.java
 * Date: 11.04.2011
 */

package scanner;

class CharacterPattern extends Pattern{

	MatchResult match(char[] in, int start) {
		int i = start;
		//TODO need to look for all symbol in for cycle, some of them can be forbidden or unused
		for (; i < in.length && in[i] != ' ' && in[i] != '{' && in[i] != '=' && 
		in[i] != ':' && in[i] != ';' && in[i] != '"' && in[i] != '(' && 
		in[i] != ')' && in[i] != '<' && in[i] != '>' && in[i] != '.' && in[i] != '-' && in[i] != '|'; ++i) { }
		if (i > start) {
			Token token;
			String s = new String(in, start, i - start);
			TokenKind k = (TokenKind) Token.command.get(s);
			if (k != null){
				token = new Token(k);
			} else {
				token = new CharacterToken(s);
			}
			return new TokenMatchResult(i - start, token);
		} else {
			return FailureMatchResult.INSTANCE;
		}
	}
}