package nlg1;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import lf.NLUUtils;
import lf.match.TimeLiteralMatcher;
import nlg1.graph.EventNode;

import org.w3c.tools.sexpr.Cons;

import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.Node;

/**
 * given a logic form, extracts a temporal ordering of events.
 *  attach subject/predicate/object(s) to each event.
 * @author morbini
 *
 */
public class MacroPlanner {
	public Node process(Object nlu) throws Exception {
		return process(nlu, null);
	}
	public EventNode process(Object nlu,String graphOutputFileName) throws Exception {
		EventNode root=buildTemporalGraph(nlu);
		addPredicationsToEvents(root,nlu);
		if (graphOutputFileName!=null) root.toGDLGraph(graphOutputFileName);
		return root;
	}
	
	/**
	 * 	for all predicates that have first argument not mentioned in the cdr of all arguments in existing seq' and par' add a new par' listing all the first arguments as cdr.
	 *  for example, if P1'(e1 LT) and P2'(e2 BT) are in the LF and e1 and e2 are not in any seq' or par' then we add a new par'(e_new_par e1 e2) 
	 * @param root
	 * @param nlu
	 * @throws Exception
	 */
	private void addPredicationsToEvents(EventNode root, Object nlu) throws Exception {
		List<Object> ps = NLUUtils.getAllPredicates(nlu);
		Set<String> visited=new HashSet<String>();
		if (ps!=null) {
			for(Object p:ps) {
				String pn=NLUUtils.getPredicateName(p);
				if (pn!=null && !pn.equalsIgnoreCase("par'") && !pn.equalsIgnoreCase("seq'")) {
					String e=NLUUtils.getEventualityName(p);
					if (e!=null) {
						e=e.toLowerCase();
						if (!visited.contains(e)) {
							visited.add(e);
							Node d = root.getDescendantNamed(e);
							if (d==null) root.addEdgeTo(d=new EventNode(e,false), true, true);
							((EventNode)d).addPredication(p);
						}
					} else throw new Exception("Predication without eventuality: "+NLUUtils.toString(p));
				}
			}
		}
	}
	
	/**
	 * get all par'/seq' predicates and their arguments (input is expected to be normalized, so all predicates are primed)
	 * the first argument of seq' and par' can be used as a shorthand to name all other arguments. That is: (par' e5 e1 e2) (seq' e4 e5 e6) means that e1 and e2 precede e6.
	 * @param nlu
	 * @return
	 * @throws Exception 
	 */
	public EventNode buildTemporalGraph(Object nlu) throws Exception {
		Map<String,List<String>> parallelSets=new HashMap<String, List<String>>(),seqSets=new HashMap<String, List<String>>();
		EventNode root=new EventNode("root",false);
		List<Object> ls=NLUUtils.extractAllPredicatesNamed(nlu,TimeLiteralMatcher.parPattern);
		addSetsTo(ls,parallelSets);
		ls=NLUUtils.extractAllPredicatesNamed(nlu, TimeLiteralMatcher.seqPattern);
		addSetsTo(ls,seqSets);
		if (!parallelSets.isEmpty()) {
			for(List<String> psa:parallelSets.values()) {
				for(String p:psa) {
					if (!parallelSets.containsKey(p)) {
						root.addEdgeTo(new EventNode(p,true), true, true);
					}
				}
			}
		}
		if (!seqSets.isEmpty()) {
			List<String> startPoint=null;
			for(List<String> ssa:seqSets.values()) {
				for(String s:ssa) {
					List<String> leaves=getLeaves(s,parallelSets,seqSets);
					if (leaves!=null) {
						if (startPoint!=null) {
							for(String x:startPoint) {
								Node xn=root.getDescendantNamed(x);
								if (xn==null) root.addEdgeTo(xn=new EventNode(x,true), true,true);
								for(String y:leaves) {
									Node yn=root.getDescendantNamed(y);
									if (yn==null) yn=new EventNode(y,true);
									xn.addEdgeTo(yn, true, true);
								}
							}
						}
						startPoint=leaves;
					}
				}
			}
		}
		cleanUnwantedRootEdges(root);
		return root;
	}

	/**
	 * removes the edge from the root to any node that has also other parents.  
	 * @param root
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 */
	private void cleanUnwantedRootEdges(Node root) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		Collection<Node> cs = root.getImmediateChildren();
		if (cs!=null) {
			for(Node c:cs) {
				List<Edge> ins = c.getIncomingEdges();
				if (ins!=null && ins.size()>1) {
					c.removeEdgeFrom(root);
				}
			}
		}
	}

	/**
	 * removes all naming eventualities and substitutes them with the actual events. For example if you have (par' e5 e1 e2) (seq' e4 e5 e6) and s=e5 then it'll return [e1. e2].
	 * @param s
	 * @param sets
	 * @return
	 */
	private List<String> getLeaves(String s, Map<String, List<String>>...sets) {
		Map<String,Boolean> seen=new HashMap<String, Boolean>();
		Stack<String> stack=new Stack<String>();
		List<String> ret=null;
		stack.push(s);
		if (sets!=null) {
			while(!stack.isEmpty()) {
				String cs=stack.pop();
				if (!seen.containsKey(cs)) {
					seen.put(cs, true);
					boolean found=false;
					for(Map<String, List<String>> set:sets) {
						if (set!=null && set.containsKey(cs)) {
							found=true;
							stack.addAll(set.get(cs));
							break;
						}
					}
					if (!found) {
						if (ret==null) ret=new ArrayList<String>();
						ret.add(cs);
					}
				}
			}
		}
		return ret;
	}

	/**
	 * expects all predications in ls to be normalized (that is, primed, sot he first argument is its eventuality)
	 * creates a map in which the keys are the eventuality names and the value are all other arguments mentiones in that predication (that is the predication with that eventuality)
	 * @param ls
	 * @param sets
	 */
	private void addSetsTo(List<Object> ls,Map<String, List<String>> sets) {
		if (ls!=null) {
			for(Object l:ls) {
				Object args = NLUUtils.cdr(l);
				boolean first=true;
				List<String> psa=null;
				for(Object a:Collections.list(((Cons)args).elements())) {
					String name=a.toString().toLowerCase();
					if (first) {
						first=false;
						psa=sets.get(name);
						if (psa==null) sets.put(name, psa=new ArrayList<String>());
					} else {
						psa.add(name);
					}
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		MacroPlanner a=new MacroPlanner();
		a.process(NLUUtils.parse("(and (enter' E1 BT) (creep' E2 BT) (par' E3 E1 E2) (moveTo' E4 C CORNER) (exit' E5 BT) (bolt' E6 BT) (par' E7 E5 E6) (seq E3 E4 E7))",true,true),"test.gdl");
	}
}
