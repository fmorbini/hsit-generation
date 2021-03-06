package nlg1.sorting;

import java.util.List;

import lf.Predication;
import nlg1.graph.EventNode;

public class NeighborsShareObjectAndPredicate implements EvalLinearization {

	float reward=0;

	public NeighborsShareObjectAndPredicate(float itemReward) {
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
					String s1 = p1.getObject();
					String s2 = p2.getObject();
					String p1n=p1.getPredicate();
					String p2n=p2.getPredicate();
					if (p1n!=null && p1n.equals(p2n)) reward+=this.reward; 
					if (s1!=null && s1.equals(s2)) reward+=this.reward;
					if (p1.getOtherObjects()!=null && p2.getOtherObjects()!=null) {
						List<String> os1 = p1.getOtherObjects();
						List<String> os2 = p2.getOtherObjects();
						int i1=os1.size();
						int i2=os2.size();
						for(int j=0;j<Math.min(i1, i2);j++) {
							String a1=os1.get(j);
							String a2=os2.get(j);
							if (a1!=null && a1.equals(a2)) {
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
