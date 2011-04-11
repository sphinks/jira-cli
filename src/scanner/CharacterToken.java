/**
 * Author: Zerin_IS
 * File: CharacterToken.java
 * Date: 11.04.2011
 */

package scanner;

class CharacterToken extends Token {

	private String value;

	CharacterToken(String value) {
		super(TokenKind.CHARACTER_LITERAL);
		this.value = value;
	}

	String getValue() {
		return value;
	}

	public String toString() {
		return super.toString() + ": " + value;
	}
}
