package nlg1.sorting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.util.graph.Node;
import lf.NLUUtils;
import nlg1.graph.EventNode;

public class AfterInInputMeansAfterInOrder implements EvalLinearization {

	float reward=0;
	Map<String,Integer> eventPositions=null;
	
	public AfterInInputMeansAfterInOrder(List<Node> obs,float itemReward) {
		eventPositions=extractEventsWithOrder(obs);
		this.reward=itemReward;
	}
	
	private Map<String, Integer> extractEventsWithOrder(List<Node> obs) {
		Map<String,Integer> ret=null;
		if (obs!=null) {
			int i=0;
			for(Node ob:obs) {
				Object nlu=NLUUtils.parse(ob.getName(), false, false);
				String ev=NLUUtils.getEventualityName(nlu);
				if (ev!=null) {
					ev=normalize(ev);
					if (ret==null) ret=new HashMap<String, Integer>();
					ret.put(ev, i);
					i++;
				}
			}
		}
		return ret;
	}
	
	private String normalize(String ev) {
		if (ev!=null) ev=ev.toLowerCase();
		return ev;
	}

	@Override
	public float reward(List<EventNode> linearization,boolean debug) {
		float reward=0;
		if (eventPositions!=null) {
			int s=linearization.size();
			for(int i=0;i<s-1;i++) {
				for(int j=i+1;j<s;j++) {
					EventNode n1=linearization.get(i);
					EventNode n2=linearization.get(j);
					Integer p1=eventPositions.get(normalize(n1.getName()));
					Integer p2=eventPositions.get(normalize(n2.getName()));
					if (p1!=null && p2!=null && p1<=p2) {
						if (debug) System.err.println(this.getClass().getSimpleName()+":"+n1+"-"+n2);
						reward+=this.reward;
					}
				}
			}
		}
		return reward;
	}

}
