package lf.match;

import edu.usc.ict.nl.util.graph.Node;

public class ANDMatcher implements LiteralMatcher {

	private LiteralMatcher[] ands;

	public ANDMatcher(LiteralMatcher... literalMatchers) {
		this.ands=literalMatchers;
	}
	
	@Override
	public boolean matches(Object nlu) {
		for(LiteralMatcher a:ands) {
			if (!a.matches(nlu)) return false;
		}
		return true;
	}

	@Override
	public boolean matches(Node lfNode) {
		for(LiteralMatcher a:ands) {
			if (!a.matches(lfNode)) return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		ret.append("(and ");
		for(LiteralMatcher a:ands) ret.append(a.toString());
		ret.append(")");
		return ret.toString();
	}

}
