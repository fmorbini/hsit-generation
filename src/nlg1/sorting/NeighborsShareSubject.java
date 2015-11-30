package nlg1.sorting;

import java.util.List;

import lf.Predication;
import nlg1.graph.EventNode;
import edu.usc.ict.nl.util.graph.Node;

public class NeighborsShareSubject implements EvalLinearization {

	float reward=0;

	public NeighborsShareSubject(float itemReward) {
		this.reward=itemReward;
	}
	
	@Override
	public float reward(List<EventNode> linearization,boolean debug) throws Exception {
		float reward=0;
		int s=linearization.size();
		for(int i=0;i<s-1;i++) {
			EventNode n1=linearization.get(i);
			EventNode n2=linearization.get(i+1);
			// assumption is that each EventNode has only one predication (if multiple the node has been split into parallel nodes each with a single predication).
			List<Predication> ps1 = n1.getPredications();
			List<Predication> ps2 = n2.getPredications();
			if (ps1!=null && ps1.size()==1) {
				if (ps2!=null && ps2.size()==1) {
					Predication p1=ps1.get(0);
					Predication p2=ps2.get(0);
					String s1 = p1.getSubject();
					String s2 = p2.getSubject();
					if (s1.equals(s2)) {
						if (debug) System.err.println(this.getClass().getSimpleName()+":"+n1+"-"+n2);
						reward+=this.reward;
					}
				}
			}
		}
		return reward;
	}

}
