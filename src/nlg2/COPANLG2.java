package nlg2;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.w3c.tools.sexpr.Cons;
import org.w3c.tools.sexpr.Symbol;

import align.COPAAligner;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.GraphElement;
import edu.usc.ict.nl.util.graph.Node;
import lf.NLUUtils;
import lf.Predication;
import lf.graph.NLUGraphUtils;
import lf.graph.WFF;
import lf.graph.dot.DotUtils;
import lf.match.ANDMatcher;
import lf.match.ArgumentOfOtherMatcher;
import lf.match.LiteralMatcher;
import lf.match.NotMatcher;
import lf.match.TimeLiteralMatcher;
import lf.match.VerbMatcher;
import lf.pos.POS;
import nlg1.MacroPlanner;
import nlg1.MicroPlanner;
import nlg1.Realizer;
import nlg1.graph.EventNode;
import nlg1.sorting.AfterInInputMeansAfterInOrder;
import nlg1.sorting.AfterInTimeMeansAfterInOrder;
import nlg1.sorting.NeighborsShareObjectAndPredicate;
import nlg1.sorting.NeighborsShareSubject;
import nlg1.sorting.NoExplanationFirst;
import nlg1.sorting.NoOrderButCommonAncestorMeansAfterInTime;

/**
 * input proof graph
 *  observations + inferences
 * finds chains of explanations
 * finds arguments dependencies. if variable is an event, then the dependent is an ancestor
 *  if variable is not an event, it's unclear if there is a dependency. There is a relation but maybe not a dependency. for NN-ADJs all literals are on teh same level
 * for each argument find set of literals to be used for verbalization. then run the set of event through the ordering machinery and then build the syntax.
 *  if event, get the literal defining that event and all modifying things: if main literal is a noun, then get adjectives. Adjectives usually modify the thing not the event.
 *   if it's a clause, then get the adverbs, they modify the event. 
 * 
 * @author morbini
 *
 */
public class COPANLG2 {

	private MacroPlanner macro;
	private MicroPlanner micro;
	private Realizer realizer;
	private NLG2Lexicon lex=null;
	private int debug=2;
	private boolean forNLG3=false;
	private PrintStream out=null;

	public COPANLG2(NLG2Lexicon lex,PrintStream out,boolean replaceWithNames) {
		NLUUtils.replaceWithNames=replaceWithNames;
		setOut(out);
		this.lex=lex;
		macro=new MacroPlanner();
		micro=new MicroPlanner();
		realizer=new Realizer();
	}
	public COPANLG2(String pFile,PrintStream out) {
		this(new NLG2Lexicon(pFile),out,false);
	}
	public void setDebug(int debug) {
		this.debug = debug;
	}

	public void setOut(PrintStream out) {
		if (out==null) out=System.out;
		this.out=out;
	}

	public NLG2Lexicon getLexicon() {
		return lex;
	}
	public Realizer getRealizer() {
		return realizer;
	}

	public void setForNLG3(boolean forNLG3) {
		this.forNLG3 = forNLG3;
	}
	public boolean getForNLG3() {return forNLG3;}

	/**
	 * given a proof
	 *  get the observations
	 *  compute the subset of observations that are clauses.
	 *   a predicate that is a verb with subject and object(s)
	 *   -(x)remove all clauses that are arguments of other predications
	 *   -classify all other predications as either modifications or arguments or predicates or subordinate/relative clauses.
	 *   => use NLG1 criteria to sort the sentences in the subset after point x.
	 *    compose the sentence structure by using the classification above (S/ADJ/ADV/REL) and ordering.
	 *  get the chains of explanations
	 * @param proof
	 * @return
	 */
	public String process(File proof) {
		try {
			NLG2Data result=getSyntaxPrecursor(proof);
			COPANLG2Generator generator = new COPANLG2Generator(result,getForNLG3(),getLexicon(),debug,out);
			List<POS> order=generator.generateSyntax();
			return processSyntax(order);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public String processSyntax(List<POS> order) {
		return realizer.process(order);
	}
	public String processSyntax(POS o) {
		List<POS> order=new ArrayList<POS>();
		order.add(o);
		return realizer.process(order);
	}

	public NLG2Data getSyntaxPrecursor(List<Node> obs) throws Exception {
		System.out.println("Input observations: "+DotUtils.extractLF(obs,false));
		normalizeProofGraph(obs);


		String nlu=DotUtils.extractLF(obs,true); // entire LF with inferences and time predicates.
		Object nluObj=NLUUtils.parse(nlu,true,false);
		System.out.println(NLUUtils.toString(nluObj));
		Map<String,Symbol> groupsOfEqualities=NLUUtils.extractEqualityGroups(nluObj);
		Node lf=NLUUtils.parseToNode(nluObj);
		if (debug>1) lf.toGDLGraph("lf-before-dep-standardization.gdl");
		NLUGraphUtils.standardizeDependencies(lf,lex);
		if (debug>1) lf.toGDLGraph("lf.gdl");

		List<Node> obsNoTimePreds=new ArrayList<Node>(obs);
		removeLiteralsThatAre(obsNoTimePreds, new TimeLiteralMatcher());
		String nluObsOnly=DotUtils.extractLF(obsNoTimePreds,false); // LF that contains only observations
		Node nluObsOnlyLF=NLUUtils.parseToNode(NLUUtils.parse(nluObsOnly,false,false));
		NLUGraphUtils.standardizeDependencies(nluObsOnlyLF,lex);
		List<Node> rootObs=new ArrayList<Node>(obs);
		removeLiteralsThatAre(rootObs,new ArgumentOfOtherMatcher(nluObsOnlyLF));
		removeLiteralsThatAre(rootObs,new ANDMatcher(new NotMatcher(new VerbMatcher(lex)),
				new NotMatcher(new TimeLiteralMatcher())));// remove all literals that are not sentences and not time literals and arguments of others (eventuality only).
		String rootnlu=DotUtils.extractLF(rootObs,false);
		System.out.println("Independent literals: "+rootnlu);
		Object independentLiterals=NLUUtils.parse(rootnlu, true,false);

		//select the list of literals to be sorted, the rest of the literals will be pulled in using the arguments
		// literals that are adj or adverbs (rb) should be taken out, only S should be left in.

		EventNode tg=macro.process(independentLiterals,(debug>1)?"time.gdl":null);
		List<Predication> independentLiteralsOrder=micro.process(tg,null,new AfterInTimeMeansAfterInOrder(10),new NoOrderButCommonAncestorMeansAfterInTime(obs, 2),
				new NoExplanationFirst(obs, 0.4f),
				new NeighborsShareSubject(0.2f),new NeighborsShareObjectAndPredicate(0.2f),
				new AfterInInputMeansAfterInOrder(obs,0.1f)
				);
		System.out.println("Independent literals order: "+independentLiteralsOrder);

		List<Node> explanations = getExplanationChains(obsNoTimePreds,lf);
		//if (debug>1) if (explanations!=null) for(Node n:explanations) n.toGDLGraph(n.getName().replaceAll("[\\s]+", "_")+".gdl");
		Map<Predication,Node> associatedExplanations=attachExplanationToPredications(independentLiteralsOrder,explanations);
		return new NLG2Data(independentLiteralsOrder,lf,groupsOfEqualities,associatedExplanations,obs);
	}
	public NLG2Data getSyntaxPrecursor(String...obs) throws Exception {
		if (obs!=null) {
			List<Node> nodes=new ArrayList<Node>();
			for(String o:obs) {
				nodes.add(new Node(o));
			}
			return getSyntaxPrecursor(nodes);
		}
		return null;
	}
	public NLG2Data getSyntaxPrecursor(InputStream in) throws Exception {
		List<Node> obs=DotUtils.read(in);
		return getSyntaxPrecursor(obs);
	}
	public NLG2Data getSyntaxPrecursor(File proof) throws Exception {
		return getSyntaxPrecursor(new FileInputStream(proof));
	}

	/**
	 * 
	 * @param obs this is a proof graph as read by {@link DotUtils}
	 * 
	 *  finds all literals and for the literals adds and modifies the references to the constants with logic form.
	 *  add the new literals introduced by the normalization as additional observations.
	 * @throws Exception 
	 *  
	 */
	private void normalizeProofGraph(List<Node> obs) throws Exception {
		replaceCOPAConstants(obs);
		replaceCOPAPredicates(obs);
	}
	private void replaceCOPAPredicates(List<Node> obs) throws Exception {
		String boxArgName=null;
		String lf = DotUtils.extractLF(obs, true);
		Object lfObj=NLUUtils.parse(lf, false,false);
		Map<String, Symbol> sys = NLUUtils.getSymbols(lfObj);
		List<Object> box = NLUUtils.extractAllPredicatesNamed(lfObj,"box");
		if (box!=null) {
			if (box.size()>1) throw new Exception("multiple box definitions.");
			for(Object b:box) {
				Object arg = NLUUtils.getMonadicPredicateArgument(b);
				boxArgName=NLUUtils.toString(arg);
			}
		}
		if (boxArgName==null) {
			Symbol arg=NLUUtils.createNewSymbol("c", sys);
			Symbol ev=NLUUtils.createNewSymbol("e", sys);
			boxArgName=NLUUtils.toString(arg);
			Node nob=new Node("(box-nn' "+ev+" "+boxArgName+")");
			obs.add(nob);
		}

		Set<Node> visited=null;
		if (obs!=null) {
			Stack<GraphElement> s=new Stack<GraphElement>();
			s.addAll(obs);
			while(!s.empty()) {
				GraphElement n=s.pop();
				if (n instanceof Edge) {
					s.add(((Edge) n).getSource());
					s.add(((Edge) n).getTarget());
				} else if (visited==null || !visited.contains(n)) {
					if (visited==null) visited=new HashSet<Node>();
					visited.add((Node) n);
					Collection<Edge> es=((Node) n).getEdges();
					if (es!=null) s.addAll(((Node) n).getEdges());
					if (DotUtils.isLiteralNode((Node) n)) {
						String l=((Node) n).getName();
						Object nluObj=NLUUtils.parse(l, true, false);
						String name=NLUUtils.getPredicateName(nluObj);
						if (isCopaPredicate(name)) {
							List<String> newArgPredications=adjustCOPAPredicates(nluObj,boxArgName);
							((Node) n).setName(NLUUtils.toString(nluObj));
							if (newArgPredications!=null) {
								for(String p:newArgPredications) {
									Node nob=new Node(p);
									obs.add(nob);
								}
							}
						}
					}
				}
			}
		}		
	}
	private List<String> adjustCOPAPredicates(Object nluObj, String boxArgName) {
		Object lastarg=NLUUtils.last(nluObj);
		if (lastarg!=null && lastarg instanceof Cons) {
			((Cons)lastarg).right(new Cons(Symbol.makeSymbol(boxArgName, null)));
		}
		return null;
	}

	private void replaceCOPAConstants(List<Node> obs) {
		Map<String,Symbol> constantToNewArg=null;
		Map<String,Object> constantToNewObs=null;
		String nlu=DotUtils.extractLF(obs,true); // entire LF with inferences and time predicates.
		Object nluObj=NLUUtils.parse(nlu, true,false);
		Map<String, Symbol> allSymbols=NLUUtils.getSymbols(nluObj);

		Set<Node> visited=null;
		if (obs!=null) {
			Stack<GraphElement> s=new Stack<GraphElement>();
			s.addAll(obs);
			while(!s.empty()) {
				GraphElement n=s.pop();
				if (n instanceof Edge) {
					s.add(((Edge) n).getSource());
					s.add(((Edge) n).getTarget());
				} else if (visited==null || !visited.contains(n)) {
					if (visited==null) visited=new HashSet<Node>();
					visited.add((Node) n);
					Collection<Edge> es=((Node) n).getEdges();
					if (es!=null) s.addAll(((Node) n).getEdges());
					if (DotUtils.isLiteralNode((Node) n)) {
						String l=((Node) n).getName();
						nluObj=NLUUtils.parse(l, false, false);
						if (nluObj!=null && nluObj instanceof Cons) {
							Object cdr=NLUUtils.cdr(nluObj);
							while(cdr!=null && cdr instanceof Cons) {
								String name=NLUUtils.toString(NLUUtils.car(cdr));
								if (isCopaConstant(name)) {
									name=name.toUpperCase();
									if (constantToNewArg==null) constantToNewArg=new HashMap<String, Symbol>();
									Symbol replacementVar=constantToNewArg.get(name);
									if (replacementVar==null) {
										replacementVar=NLUUtils.createNewSymbol("c",allSymbols);
										constantToNewArg.put(name, replacementVar);
									}
									if (constantToNewObs==null) constantToNewObs=new HashMap<String, Object>();
									Object newArgPredications=constantToNewObs.get(name);
									if (newArgPredications==null) {
										newArgPredications=NLUUtils.buildExtendedCopaConstantReplacement(name,replacementVar,allSymbols);
										constantToNewObs.put(name, newArgPredications);
										for(Object p:NLUUtils.asList(newArgPredications)) {
											Node nob=new Node(NLUUtils.toString(p));
											obs.add(nob);
											s.add(nob);
										}
									}
									assert(replacementVar!=null && newArgPredications!=null);
									NLUUtils.car(cdr,replacementVar);
									((Node) n).setName(NLUUtils.toString(nluObj));
								}
								cdr=NLUUtils.cdr(cdr);
							}
						}
					}
				}
			}
		}		
	}
	private void replaceEqualities(List<Node> obs) {
		try {
			Map<String,Symbol> groupsOfEqualities=extractEqualityGroups(obs);
			if (groupsOfEqualities!=null) {
				Set<Node> visited=null;
				if (obs!=null) {
					Stack<GraphElement> s=new Stack<GraphElement>();
					s.addAll(obs);
					while(!s.empty()) {
						GraphElement n=s.pop();
						if (n instanceof Edge) {
							s.add(((Edge) n).getSource());
							s.add(((Edge) n).getTarget());
						} else if (visited==null || !visited.contains(n)) {
							if (visited==null) visited=new HashSet<Node>();
							visited.add((Node) n);
							Collection<Edge> es=((Node) n).getEdges();
							if (es!=null) s.addAll(((Node) n).getEdges());
							if (DotUtils.isLiteralNode((Node) n)) {
								String l=((Node) n).getName();
								Object nluObj=NLUUtils.parse(l, false, false);
								nluObj=NLUUtils.parse(l, false, false);
								if (nluObj!=null && nluObj instanceof Cons) {
									Object cdr=NLUUtils.cdr(nluObj);
									while(cdr!=null && cdr instanceof Cons) {
										String name=NLUUtils.toString(((Cons)cdr).left());
										Symbol rep=groupsOfEqualities.get(name);
										if (rep!=null) {
											((Cons)cdr).left(rep);
										}
										cdr=NLUUtils.cdr(cdr);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * returns a map in which the keys are the variable names found in the input formula and the value is the new name given to all the variables in that equality group.
	 * @param obs
	 * @return
	 * @throws Exception
	 */
	private Map<String, Symbol> extractEqualityGroups(List<Node> obs) throws Exception {
		String nlu=DotUtils.extractLF(obs,true); // entire LF with inferences and time predicates.
		Object nluObj=NLUUtils.parse(nlu, false,false);
		return NLUUtils.extractEqualityGroups(nluObj);
	}

	private boolean isCopaConstant(String aName) {
		return aName.equalsIgnoreCase("BT")||aName.equalsIgnoreCase("LT")||aName.equalsIgnoreCase("C")||aName.equalsIgnoreCase("D")||aName.equalsIgnoreCase("B")||aName.equalsIgnoreCase("CORNER")||aName.equalsIgnoreCase("OUTSIDE")||aName.equalsIgnoreCase("BEHINDBOX");
	}
	private boolean isCopaPredicate(String aName) {
		return aName.equals("inside'")||aName.equals("outside'")||aName.equals("exit'")||aName.equals("enter'");
	}
	


	
	public static Set<String> getPredicationsInExplanationChain(Node explanation,Set<String> ret) {
		if (explanation!=null && explanation.hasParents()) {
			try {
				Collection<Node> parents=explanation.getParents();
				if (parents!=null && !parents.isEmpty()) {
					for (Node p:parents) {
						ret.add(p.getName());
						getPredicationsInExplanationChain(p,ret);
					}
				}
			} catch (Exception e) {e.printStackTrace();}
		}
		return ret;
	}

	/**
	 * 
	 * @param independentLiteralsOrder list of predications.
	 * @param explanations list of nodes corresponding to the observations only. each node is a leaf in a tree that represents the explanation of that observation.
	 * @return a map with key each predication and value the observation node associated with it (with its explanation)
	 */
	private Map<Predication, Node> attachExplanationToPredications(List<Predication> observations, List<Node> explanations) {
		Map<Predication, Node> ret=null;
		if (explanations!=null) {
			Map<String,Node> exp=new HashMap<String, Node>();
			for(Node e:explanations) exp.put(e.getName(),e);
			for(Predication o:observations) {
				Object oNLU=o.getSource();
				String pName=NLUUtils.toString(oNLU);
				Node e=exp.get(pName);
				if (e!=null) {
					if (ret==null) ret=new HashMap<Predication, Node>();
					ret.put(o, e);
				}
			}
		}
		return ret;
	}

	private void removeLiteralsThatAre(List<Node> obs,LiteralMatcher keep) throws Exception {
		if (obs!=null) {
			Iterator<Node> it=obs.iterator();
			while(it.hasNext()) {
				Node ob=it.next();
				Object nlu=NLUUtils.parse(ob.getName(), false,false);
				if (keep.matches(nlu)) {
					if (debug>1) System.out.println("removing literal: "+ob+" because of "+keep);
					it.remove();
				}
			}
		}
	}

	public static void printAllPredicates() throws Exception {
		List<Object> obj = NLUUtils.parse(new File("paetc/TriCOPA-kb.lisp"), true);
		List<Object> ps = NLUUtils.getAllPredicates(obj);
		Set<String> predicates=new HashSet<String>();
		for(Object p:ps) {
			String name=NLUUtils.getPredicateName(p);
			int as=NLUUtils.length(p);
			predicates.add(name+"_"+as);
		}

		List<File> fs = FileUtils.getAllFiles(new File("paetc/prbs/"), ".*\\.dot");
		for(File f:fs) {
			List<Node> obs=DotUtils.read(f);
			String nlu=DotUtils.extractLF(obs,false);
			Object nluObj=NLUUtils.parse(nlu, true,false);
			ps = NLUUtils.getAllPredicates(nluObj);
			for(Object p:ps) {
				String name=NLUUtils.getPredicateName(p);
				int as=NLUUtils.length(p);
				predicates.add(name+"_"+as);
			}
		}

		List<TrainingDataFormat> tds=COPAAligner.createNLUTDfromCOPAQuestions(new File("data/TriCOPA.txt"));
		for(TrainingDataFormat td:tds) {
			String nlu=(td.getLabel());
			Object nluObj=NLUUtils.parse(nlu, true,false);
			ps = NLUUtils.getAllPredicates(nluObj);
			for(Object p:ps) {
				String name=NLUUtils.getPredicateName(p);
				int as=NLUUtils.length(p);
				predicates.add(name+"_"+as);
			}
		}

		List<String> sortedPredicates=new ArrayList<String>(predicates);
		Collections.sort(sortedPredicates);
		System.out.println(FunctionalLibrary.printCollection(sortedPredicates, "", "", "\n"));
	}

	/**
	 * for each observation in obs.
	 *  get all parents that are inferences (no numbers)
	 *  for all points with more than 1 parent see if the parents are related in the lf graph (by argument relations).
	 *   if they are related, detach the parent edge and attach to the related parent.
	 * @param obs
	 * @param lf
	 * @return
	 */
	public List<Node> getExplanationChains(List<Node> obs,Node lf) {
		List<Node> ret=null;

		for(Node ob:obs) {
			String fileName=ob.getName().replaceAll("[\\s]+", "_");
			if (debug>1) ob.toGDLGraph(fileName+"-before.gdl");
			Node obE=getRawExplanationSubgraphFor(ob);
			if (debug>1) obE.toGDLGraph(fileName+"-after.gdl");
			applyArgumentRelations(obE,lf); 
			if (debug>1) obE.toGDLGraph(fileName+"-after2.gdl");
			int numNodes=obE.countNodes();
			if (numNodes>1) {
				boolean singleAncestorChain=true;
				Node x=obE;
				try {
					while((x=x.getSingleParent())!=null) {
						if (!x.hasParents()) break;
					}
				} catch (Exception e) {
					singleAncestorChain=false;
				}
				if (ret==null) ret=new ArrayList<Node>();
				ret.add(obE);
			}
		}
		return ret;
	}

	/**
	 * traverse the ancestors of obE.
	 *  compute all pairs in ancestors
	 *   for each pair mark if one is the argument of the other (direct or indirect).
	 *    for each of the nodes that are arguments, <mp,p> (mp uses p as argument):
	 *     for each child c of p
	 *      if c has multiple parents, (one is p) and at least another has as ancestor mp. Remove the link c-p
	 *     if p has no more children, attach it as child of mp.
	 * @param ob it's the explanation graph
	 * @param lf it's the logic form graph (includes all inferences)
	 */
	private void applyArgumentRelations2(Node ob, Node lf) {
		Set<Node> nodes = ob.getAllNodes();
		nodes.remove(ob);
		Map<Node,Node> explanationToLF=computeNodeMap(ob,lf);
		for(Node p:nodes) {
			Node lfp=explanationToLF.get(p);
			for(Node mp:nodes) {
				if (mp!=p && mp!=ob) {
					Node lfMp=explanationToLF.get(mp);
					Integer distance=lfMp.getDistanceTo(lfp);
					if (distance!=null) {
						try {
							Collection<Node> children = p.getImmediateChildren();
							for(Node c:children) {
								if (c.hasThisAncestor(mp)) {
									c.removeEdgeFrom(p);
								}
							}
							if (!p.hasChildren()) {
								mp.addEdgeTo(p, true, true);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
				}
			}
		}
	}
	/**
	 * for every pair of nodes excluding the observation node, if the parent uses the eventuality defined by the children in its arguments, then 
	 * @param explanationGraph explanation graph (from abduction)
	 * @param lf logic form graph (argument relations)
	 */
	private void applyArgumentRelations(Node explanationGraph, Node lf) {
		Set<Node> nodes = explanationGraph.getAllNodes();
		nodes.remove(explanationGraph);
		Map<Node,Node> explanationToLF=computeNodeMap(explanationGraph,lf);
		for(Node p:nodes) {
			Node lfp=explanationToLF.get(p);
			for(Node mp:nodes) {
				if (mp!=p && mp!=explanationGraph) {
					Node lfMp=explanationToLF.get(mp);
					Integer distance=lfMp.getDistanceTo(lfp);
					if (distance!=null) {
						Object nlu=((WFF)lfp).getParsedNLUObject(false);
						String ev=NLUUtils.getEventualityName(nlu);
						nlu=((WFF)lfMp).getParsedNLUObject(false);
						try {
							Set<String> mpArgs=new HashSet<String>(FunctionalLibrary.map(NLUUtils.getArguments(nlu), NLUUtils.class.getMethod("toString", Object.class)));
							if (mpArgs.contains(ev)) {
								Collection<Node> children = p.getImmediateChildren();
								for(Node c:children) {
									//if (c.hasThisAncestor(mp)) {
									c.removeEdgeFrom(p);
									//}
								}
								//if (!p.hasChildren()) {
								mp.addEdgeTo(p, true, true);
								//}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * returns a map from nodes in the from graph to the corresponding nodes in the to graph. Corresponding means with the same name.
	 * @param from
	 * @param to
	 * @return
	 */
	private Map<Node, Node> computeNodeMap(Node from, Node to) {
		Map<String,Node> fromMap=new HashMap<String, Node>(),toMap=new HashMap<String, Node>();
		Set<Node> nodes=from.getAllNodes();
		for(Node n:nodes) fromMap.put(n.getName(), n);
		nodes=to.getAllNodes();
		for(Node n:nodes) toMap.put(n.getName(), n);
		Map<Node,Node> ret=new HashMap<Node, Node>();
		for(String fromName:fromMap.keySet()) {
			Node toNode=toMap.get(fromName);
			ret.put(fromMap.get(fromName), toNode);
		}
		return ret;
	}

	/**
	 * given the observation node, get all its parents that are not numbers.
	 * @param ob
	 * @return
	 */
	public static Node getRawExplanationSubgraphFor(Node ob) {
		Map<Node,Node> oldToNew=new HashMap<Node, Node>();
		Stack<Node> s=new Stack<Node>();
		s.push(ob);
		while(!s.isEmpty()) {
			Node x=s.pop();
			Node nx=getNewNode(x, oldToNew);
			try {
				Collection<Node> parents = x.getImmediateParents();
				if (parents!=null) {
					Stack<Node> pStack=new Stack<Node>();
					pStack.addAll(parents);
					while(!pStack.isEmpty()) {
						Node p=pStack.pop();
						String name=p.getName();
						if (StringUtils.isEmptyString(name)) {
							Collection<Node> pps = p.getImmediateParents();
							if (pps!=null) {
								pStack.addAll(pps);
							}
						} else if (DotUtils.isLiteralNode(p)) {
							s.push(p);
							getNewNode(p,oldToNew).addEdgeTo(nx, true, true);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return oldToNew.get(ob);
	}

	private static Node getNewNode(Node x, Map<Node, Node> oldToNew) {
		Node nx=oldToNew.get(x);
		if (nx==null) oldToNew.put(x,nx=new Node(x.getName()));
		return nx;
	}

	/**
	 * 
	 * @param item: starts from 0.
	 * @throws Exception 
	 */
	public static void runTriCopaItem(int item) throws Exception {
		List<TrainingDataFormat> tds=COPAAligner.createNLUTDfromCOPAQuestions(new File("data/TriCOPA.txt"));
		COPANLG2 nlg = new COPANLG2("predicateList.xlsx",null);
		File prb=new File("paetc/prbs/prb-"+(item+1)+"-proof.dot");
		String r=nlg.process(prb);
		System.out.println((item+1)+": "+tds.get(item).getId());
		System.out.println("  "+r);
	}

	/*
	private static void runSystemOn(File triCopaDataset, File problemsDir,PrintStream output) throws Exception {
		List<TrainingDataFormat> tds=COPAAligner.createNLUTDfromCOPAQuestions(triCopaDataset);
		COPANLG2 nlg = new COPANLG2("predicateList.xlsx");
		for(int i=0;i<tds.size();i++) {
			File prb=new File(problemsDir,"prb-"+(i+1)+"-proof.dot");
			String r=nlg.process(prb);
			output.println((i+1)+": "+tds.get(i).getId());
			output.println("  "+r);
		}
		output.close();
	}
	 */
	public static void main(String[] args) throws Exception {
		COPANLG2 nlg = new COPANLG2("predicateList.xlsx",null);
		NLG2Data result=nlg.getSyntaxPrecursor("(seq E1 E2),(circle-nn' e6 c1),(creepUpOn' E1 c1 BT),(flinch' E2 BT)".split(","));
		COPANLG2Generator generator=new COPANLG2Generator(result,nlg.getForNLG3(),nlg.getLexicon(),nlg.debug,nlg.out);
		List<POS> order=generator.generateSyntax();
		String ss=nlg.processSyntax(order);
		System.out.println(ss);
		System.exit(0);


		//runSystemOn(new File("data/TriCOPA.txt"),new File("paetc/prbs/"),new PrintStream(new File("test-output")));
		runTriCopaItem(14);//75
		//System.exit(0);
		//printAllPredicates();
		/*
		List<TrainingDataFormat> tds=COPAAligner.createNLUTDfromCOPAQuestions(new File("data/TriCOPA.txt"));
		int i=1;
		for(TrainingDataFormat td:tds) {
			System.out.println(td.getUtterance());
			File prb=new File("paetc/prbs/prb-"+i+"-proof.dot");
			String r=nlg.process(prb);
			System.out.println("   "+r);
			i++;
		}
		 */
		//String r2=nlg.process("(and (inside' E1 C) (outside' E2 BT) (knock' E3 BT D) (open' E4 C D) (close' E5 C D) (seq E3 E4 E5))");
		//System.out.println(r2);
		//System.exit(1);
	}
}
