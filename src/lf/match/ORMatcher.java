package lf.match;

import edu.usc.ict.nl.util.graph.Node;

public class ORMatcher implements LiteralMatcher {

	private LiteralMatcher[] ors;

	public ORMatcher(LiteralMatcher... literalMatchers) {
		this.ors=literalMatchers;
	}
	
	@Override
	public boolean matches(Object nlu) {
		for(LiteralMatcher o:ors) {
			if (o.matches(nlu)) return true;
		}
		return false;
	}

	@Override
	public boolean matches(Node lfNode) {
		for(LiteralMatcher o:ors) {
			if (o.matches(lfNode)) return true;
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		ret.append("(and ");
		for(LiteralMatcher a:ors) ret.append(a.toString());
		ret.append(")");
		return ret.toString();
	}
}
