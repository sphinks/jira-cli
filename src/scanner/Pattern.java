package scanner;

abstract class Pattern {

  abstract MatchResult match(char[] in, int start);
}