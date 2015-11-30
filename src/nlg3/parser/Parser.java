/* Generated By:JavaCC: Do not edit this line. Parser.java */
package nlg3.parser;

import java.util.ArrayList;
import java.util.List;
import nlg3.SyntaxBuilder;

/** Simple brace matcher. */
public class Parser implements ParserConstants {
        public List makeArglist(Object f) {
                if (f==null) return new ArrayList();
                else {
                        List args=new ArrayList();
                        args.add(f);
                        return args;
                }
        }
        public List makeArglist() {
                return new ArrayList();
        }
        public List makeArglist(Object f,List args2) {
                if (f==null) return null;
                else {
                        List args=new ArrayList();
                        args.add(f);
                        if (args!=null) args.addAll(args2);
                        return args;
                }
        }
        public SyntaxBuilder makeFunction(Token f,List<SyntaxBuilder> args) throws ParseException {
                String fname=f.image;
                try {
                        SyntaxBuilder rr=SyntaxBuilder.create(fname,args);
                        return rr;
                        } catch (Exception e) {
                                e.printStackTrace();
                                throw new ParseException("error creating function starting at line: "+f.beginLine+" column: "+f.beginColumn);
                        }
        }

/** Root production. */
  final public SyntaxBuilder syntax() throws ParseException {
        Token p=null,cmp=null;
        SyntaxBuilder a0=null,a1=null;
        List<SyntaxBuilder> args=null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case ID:
      p = jj_consume_token(ID);
      break;
    case AND:
      p = jj_consume_token(AND);
      break;
    case OR:
      p = jj_consume_token(OR);
      break;
    case NOT:
      p = jj_consume_token(NOT);
      break;
    default:
      jj_la1[0] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case LB:
      args = arguments();
      break;
    default:
      jj_la1[1] = jj_gen;
      ;
    }
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case CMP:
      cmp = jj_consume_token(CMP);
      a1 = syntax();
      break;
    default:
      jj_la1[2] = jj_gen;
      ;
    }
                a0=makeFunction(p,args);
                if (cmp!=null && a1!=null) {
                        args=new ArrayList<SyntaxBuilder>();
                        args.add(a0);
                        args.add(a1);
                        {if (true) return makeFunction(cmp,args);}
                } else {
                        {if (true) return a0;}
                }
    throw new Error("Missing return statement in function");
  }

  final public List<SyntaxBuilder> arguments() throws ParseException {
        List<SyntaxBuilder> args=null;
    jj_consume_token(LB);
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case AND:
    case OR:
    case NOT:
    case ID:
      args = argumentsBody();
      break;
    default:
      jj_la1[3] = jj_gen;
      ;
    }
    jj_consume_token(RB);
                                         {if (true) return (args!=null)?args:null;}
    throw new Error("Missing return statement in function");
  }

  final public List<SyntaxBuilder> argumentsBody() throws ParseException {
        SyntaxBuilder wff;
        List<SyntaxBuilder> args=null;
    wff = syntax();
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case CM:
      jj_consume_token(CM);
      args = argumentsBody();
      break;
    default:
      jj_la1[4] = jj_gen;
      ;
    }
                                                   {if (true) return (args!=null)?makeArglist(wff,args):makeArglist(wff);}
    throw new Error("Missing return statement in function");
  }

  /** Generated Token Manager. */
  public ParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private int jj_ntk;
  private int jj_gen;
  final private int[] jj_la1 = new int[5];
  static private int[] jj_la1_0;
  static {
      jj_la1_init_0();
   }
   private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {0x1380,0x20,0x800,0x1380,0x400,};
   }

  /** Constructor with InputStream. */
  public Parser(java.io.InputStream stream) {
     this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public Parser(java.io.InputStream stream, String encoding) {
    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new ParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
  }

  /** Constructor. */
  public Parser(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new ParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
  }

  /** Constructor with generated Token Manager. */
  public Parser(ParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
  }

  /** Reinitialise. */
  public void ReInit(ParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
  }

  private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }


/** Get the next Token. */
  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;

  /** Generate ParseException. */
  public ParseException generateParseException() {
    jj_expentries.clear();
    boolean[] la1tokens = new boolean[13];
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 5; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 13; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.add(jj_expentry);
      }
    }
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = jj_expentries.get(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

}
