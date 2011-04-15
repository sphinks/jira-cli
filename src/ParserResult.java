/**
 * Author: Zerin_IS
 * File: ParserResult.java
 * Date: 15.04.2011
 */
/**
 * 
 */

/**
 * @author Zerin_IS
 *
 */
public abstract class ParserResult {
	
	private String comment;
	private Rule parsedRule;
	
	public ParserResult(String comment, Rule parsedRule) {
		this.comment = comment;
		this.parsedRule = parsedRule;
	}
	
	public String getComment() {
		return comment;
	}
	public Rule getParsedRule() {
		return parsedRule;
	}
	
	

}
