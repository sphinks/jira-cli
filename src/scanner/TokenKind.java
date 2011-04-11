/**
 * Author: Zerin_IS
 * File: TokenKind.java
 * Date: 11.04.2011
 */

package scanner;


public class TokenKind {

	final static TokenKind COMMA;
	final static TokenKind COLON;
	final static TokenKind EQUALS;
	final static TokenKind SEMICOLON;
	final static TokenKind DOT;
	final static TokenKind DOT_DOT;
	final static TokenKind LESS;
	final static TokenKind MORE;
	final static TokenKind MORE_EQUALS;
	final static TokenKind LESS_EQUALS;
	final static TokenKind NOT_EQUALS;
	final static TokenKind DASH;
	final static TokenKind PIPE;

	final static TokenKind COMMAND_ISSUE;
	final static TokenKind MODIFIER;

	final static TokenKind CHARACTER_LITERAL;
	final static TokenKind INTEGER_LITERAL;
	final static TokenKind REAL_LITERAL;
	final static TokenKind STRING_LITERAL;



	private final int value;
	private final String name;

	private TokenKind(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public boolean equals(Object o) {
		return o instanceof TokenKind ? value == ((TokenKind) o).value : false;
	}

	public String toString() {
		return name;
	}

	static {
		int i = 0;
		COMMA = new TokenKind(i, ",");
		COLON = new TokenKind(i++, ":");
		EQUALS = new TokenKind(i++, "=");
		SEMICOLON = new TokenKind(i++, ";");
		DOT = new TokenKind(i++, ".");
		DOT_DOT = new TokenKind(i++, "..");
		LESS = new TokenKind(i++, "<");
		MORE = new TokenKind(i++, ">");
		LESS_EQUALS = new TokenKind(i++, "<=");
		MORE_EQUALS = new TokenKind(i++, ">=");
		NOT_EQUALS = new TokenKind(i++, "<>");
		DASH = new TokenKind(i++, "-");
		PIPE = new TokenKind(i++, "|");

		COMMAND_ISSUE = new TokenKind(i++, "issue");
		MODIFIER = new TokenKind(i++, "modifier");

		CHARACTER_LITERAL = new TokenKind(i++, "Character");
		INTEGER_LITERAL = new TokenKind(i++, "Integer");
		REAL_LITERAL = new TokenKind(i++, "Real");
		STRING_LITERAL = new TokenKind(i++, "String");
	}
}
