package nlg1.sorting;

import java.util.List;

import nlg1.graph.EventNode;
import nlg1.graph.Query;

public class AfterInTimeMeansAfterInOrder implements EvalLinearization {

	float reward=0;
	
	public AfterInTimeMeansAfterInOrder(float itemReward) {
		this.reward=itemReward;
	}
	
	@Override
	public float reward(List<EventNode> linearization,boolean debug) {
		float reward=0;
		int s=linearization.size();
		for(int i=0;i<s-1;i++) {
			for(int j=i+1;j<s;j++) {
				EventNode n1=linearization.get(i);
				EventNode n2=linearization.get(j);
				if (Query.after(n2, n1) || !Query.before(n2, n1)) {
					if (debug) System.err.println(this.getClass().getSimpleName()+":"+n1+"-"+n2);
					reward+=this.reward;
				}
			}
		}
		return reward;
	}

}
