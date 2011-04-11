/**
 * Author: Zerin_IS
 * File: IntegerToken.java
 * Date: 11.04.2011
 */

package scanner;

class IntegerToken extends Token {

	private int value;

	IntegerToken(int value) {
		super(TokenKind.INTEGER_LITERAL);
		this.value = value;
	}

	int getValue() {
		return value;
	}

	public String toString() {
		return super.toString() + ": " + value;
	}
}