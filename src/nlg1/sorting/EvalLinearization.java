package nlg1.sorting;

import java.util.List;

import nlg1.graph.EventNode;

public interface EvalLinearization {
	public float reward(List<EventNode> linearization, boolean debug) throws Exception;
}
