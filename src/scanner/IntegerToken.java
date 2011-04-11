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