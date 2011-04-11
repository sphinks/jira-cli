/**
 * Author: Zerin_IS
 * File: TokenMatchResult.java
 * Date: 11.04.2011
 */

package scanner;

public class TokenMatchResult extends SuccessMatchResult {

	private Token token;

	TokenMatchResult(int eatenCount, Token token) {
		super(eatenCount);
		this.token = token;
	}

	Token getToken() {
		return token;
	}
}