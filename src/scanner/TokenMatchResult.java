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