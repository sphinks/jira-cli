/**
 * Author: Zerin_IS
 * File: StringToken.java
 * Date: 11.04.2011
 */

package scanner;

class StringToken extends Token{

	private String value;

	StringToken(String value) {
		super(TokenKind.STRING_LITERAL);
		this.value = value;
	}

	String getValue() {
		return value;
	}

	public String toString() {
		return super.toString() + ": " + value;
	}
}