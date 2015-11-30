package lf.match;

import lf.NLUUtils;
import lf.graph.WFF;
import nlg2.NLG2Lexicon;
import edu.usc.ict.nl.util.graph.Node;

public class VerbMatcher implements LiteralMatcher {

	private NLG2Lexicon lex;

	public VerbMatcher(NLG2Lexicon lex) {
		this.lex=lex;
	}
	
	@Override
	public boolean matches(Object nlu) {
		if (nlu!=null) {
			String pname=NLUUtils.getPredicateName(nlu);
			return NLUUtils.isVerb(pname, lex);
		}
		return false;
	}

	@Override
	public boolean matches(Node lfNode) {
		if (lfNode!=null && lfNode instanceof WFF) {
			return matches(((WFF)lfNode).getParsedNLUObject(false));
		}
		return false;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
