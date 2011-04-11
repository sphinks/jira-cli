/**
 * Author: Zerin_IS
 * File: Scanner.java
 * Date: 11.04.2011
 */

package scanner;


public class Scanner {

	final static Pattern[] PATTERNS = {
		new SpacesPattern(),
		new StringPattern(),
		new PointDotPattern(),
		new NumberPattern(),
		new CharacterPattern() };

	public Token[] scan(char[] in) {
		Token[] token_array = new Token[0];
		Token[] tmp;
		int k = 0;
		for (int i = 0; i < in.length; ) {
			SuccessMatchResult longestResult = null;
			for (int j = 0; j < PATTERNS.length; ++j) {
				MatchResult r = PATTERNS[j].match(in, i);
				if (r instanceof SuccessMatchResult) {
					SuccessMatchResult result = (SuccessMatchResult) r;
					if (longestResult == null || result.getEatenCount() > longestResult.getEatenCount()) {
						longestResult = result;
					}
				}
			}
			if (longestResult != null) {
				if (longestResult instanceof TokenMatchResult) {
					tmp = new Token[k];
					System.arraycopy(token_array,0,tmp,0,k);
					token_array = new Token[k+1];
					System.arraycopy(tmp,0,token_array,0,k);
					token_array[k] = (((TokenMatchResult)longestResult).getToken());
					k++;
				}
				i += longestResult.getEatenCount();
			} else {
				System.out.println("Error");
				return null;
			}
		}
		return token_array;
	}
}  