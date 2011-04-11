package scanner;

class RealToken extends Token {

  private double value;

  RealToken(double value) {
    super(TokenKind.REAL_LITERAL);
    this.value = value;
  }

  double getValue() {
    return value;
  }
  
  public String toString() {
    return super.toString() + ": " + value;
  }
}
