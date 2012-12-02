package jiracli.common;
/**
 * Author: Zerin_IS
 * File: FailedParserResult.java
 * Date: 15.04.2011
 */
/**
 * 
 */

/**
 * @author Zerin_IS
 *
 */
public class FailedParserResult extends ParserResult {
	
	public FailedParserResult(Rule supposeRule) {
		super("Cann`t find exact rule. May be you mean:", supposeRule);
	}

}
