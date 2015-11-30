package nlg1.sorting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.util.graph.Node;
import lf.NLUUtils;
import lf.graph.dot.DotUtils;
import nlg1.graph.EventNode;

public class NoOrderButCommonAncestorMeansAfterInTime implements EvalLinearization {

	float reward=0;
	Map<String,Set<String>> relations=null;
	
	public NoOrderButCommonAncestorMeansAfterInTime(List<Node> obs,float itemReward) {
		for(Node o:obs) {
			Object nlu=NLUUtils.parse(o.getName(), false, false);
			String evo=NLUUtils.getEventualityName(nlu);
			if (evo!=null) {
				evo=normalize(evo);
				List<Node> oas = DotUtils.removeNonLiterals(o.getAllAncestors());
				if (oas!=null && ! oas.isEmpty()) {
					for(Node oo:obs) {
						nlu=NLUUtils.parse(oo.getName(), false, false);
						String evoo=NLUUtils.getEventualityName(nlu);
						if (evoo!=null) {
							evoo=normalize(evoo);
							List<Node> ooas = DotUtils.removeNonLiterals(oo.getAllAncestors());
							if (ooas!=null) {
								Set<Node> oasSet=new HashSet<Node>(oas);
								oasSet.retainAll(ooas);
								if (!oasSet.isEmpty()) {
									if (relations==null) relations=new HashMap<String,Set<String>>();
									Set<String> rs = relations.get(evo);
									if (rs==null) relations.put(evo, rs=new HashSet<String>());
									rs.add(evoo);
								}
							}
						}
					}
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
		if (relations!=null) {
			int s=linearization.size();
			for(int i=0;i<s-1;i++) {
				for(int j=i+1;j<s;j++) {
					EventNode n1=linearization.get(i);
					EventNode n2=linearization.get(j);
					if (n1.getKnownTimeRelation() && !n2.getKnownTimeRelation()) {
						Set<String> p1=relations.get(normalize(n1.getName()));
						Set<String> p2=relations.get(normalize(n2.getName()));
						if (p1!=null && !p1.isEmpty() && p2!=null && !p2.isEmpty()) {
							boolean intersection=false;
							for(String np1:p1) if (p2.contains(np1)) {
								intersection=true;
								break;
							}
							if (intersection) {
								if (debug) System.err.println(this.getClass().getSimpleName()+":"+n1+"-"+n2);
								reward+=this.reward;
							}
						}
					}
				}
			}
		}
		return reward;
	}

}
