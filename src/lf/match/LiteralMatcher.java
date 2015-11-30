package lf.match;

import edu.usc.ict.nl.util.graph.Node;

public interface LiteralMatcher {
	public boolean matches(Object nlu);
	public boolean matches(Node lfNode);
}
