/**
 * Author: Zerin_IS
 * File: Token.java
 * Date: 11.04.2011
 */

package scanner;

import java.util.*;

public class Token{

	static Map<String, TokenKind> command = new HashMap<String, TokenKind>();

	private TokenKind kind;

	Token(TokenKind kind) {
		this.kind = kind;
	}

	public TokenKind getKind() {
		return kind;
	}

	static {
		command.put("issue", TokenKind.COMMAND_ISSUE);
	}

	public String toString() {
		return kind.toString();
	}
}
