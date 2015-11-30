package nlg1.sorting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.usc.ict.nl.util.graph.Node;
import lf.NLUUtils;
import lf.graph.dot.DotUtils;
import nlg1.graph.EventNode;

public class NoExplanationFirst implements EvalLinearization {

	float reward=0;
	Set<String> withExplanation=null;

	public NoExplanationFirst(List<Node> obs,float itemReward) {
		for(Node o:obs) {
			Object nlu=NLUUtils.parse(o.getName(), false, false);
			String evo=NLUUtils.getEventualityName(nlu);
			if (evo!=null) {
				evo=normalize(evo);
				List<Node> oas = DotUtils.removeNonLiterals(o.getAllAncestors());
				if (oas!=null && !oas.isEmpty()) {
					if (withExplanation==null) withExplanation=new HashSet<String>();
					withExplanation.add(evo);
				}
			}
		}
		this.reward=itemReward;
	}

	private String normalize(String ev) {
		if (ev!=null) ev=ev.toLowerCase();
		return ev;
	}

	@Override
	public float reward(List<EventNode> linearization,boolean debug) {
		float reward=0;
		if (withExplanation!=null) {
			for(EventNode n1:linearization) {
				boolean p1=withExplanation.contains(normalize(n1.getName()));
				if (p1) break;
				else {
					if (debug) System.err.println(this.getClass().getSimpleName()+":"+n1);
					reward+=this.reward;
				}
			}
		}
		return reward;
	}
}
