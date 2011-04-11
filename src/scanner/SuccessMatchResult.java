/**
 * Author: Zerin_IS
 * File: SuccessMatchResult.java
 * Date: 11.04.2011
 */

package scanner;

abstract class SuccessMatchResult extends MatchResult {

	private int eatenCount;

	SuccessMatchResult(int eatenCount) {
		this.eatenCount = eatenCount;
	}

	int getEatenCount() {
		return eatenCount;
	}
}
