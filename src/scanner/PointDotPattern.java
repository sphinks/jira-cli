package scanner;

class PointDotPattern extends Pattern{
  
    MatchResult match(char[] in, int start) {
    int i = start;
    TokenKind k;
    switch (in[start]){
            case ':':
            i++;
            k = TokenKind.COLON;
            break;
            
            case ';':
            i++;
            k = TokenKind.SEMICOLON;
            break;
            
            case '=':
            i++;
            k = TokenKind.EQUALS;
            break;
            
            case ',':
            i++;
            k = TokenKind.COMMA;
            break;
            
            case '|':
            i++;
            k = TokenKind.PIPE;
            break;
            
            case '-':
            i++;
            k = TokenKind.DASH;
            break;
            
            case '<':
            i++;
            if (in.length - start > 1 && in[start+1] == '>'){
              i++;
              k = TokenKind.NOT_EQUALS;
            } else {
              if (in.length - start > 1 && in[start+1] == '='){ 
                i++;
                k = TokenKind.LESS_EQUALS;
              } else {
                k = TokenKind.LESS;
              }
            }
            break;
            
            case '>':
            i++;
            if (in.length - start > 1 && in[start+1] == '='){
              i++;
              k = TokenKind.MORE_EQUALS;
            } else {
              k = TokenKind.MORE;  
            }
            break;
            
            case '.':
            if (in.length - start > 1 && in[start+1] == '.'){
                //System.out.println("dot_dot");
                i += 2;
                k = TokenKind.DOT_DOT;
            } else {
                i++;
                k = TokenKind.DOT;
            }
            break;
            
            default:
                k = null;
        }
        
  if (i > start && k != null) {
      return new TokenMatchResult(i - start, new Token(k));
    } else {
      return FailureMatchResult.INSTANCE;
    }
  }
}