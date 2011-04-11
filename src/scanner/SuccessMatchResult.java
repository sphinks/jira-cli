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
