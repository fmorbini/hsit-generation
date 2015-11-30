package nlg2;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.tools.sexpr.Symbol;

import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.graph.Node;
import lf.NLUUtils;
import lf.Predication;
import lf.graph.NLUGraphUtils;
import lf.graph.WFF;
import lf.pos.Coordination;
import lf.pos.NP;
import lf.pos.POS;
import lf.pos.POSI.DT;
import lf.pos.PP;
import lf.pos.Sentence;
import lf.pos.VP;

public class COPANLG2Generator {
	private List<Predication> independentLiteralsOrder;
	private Map<Predication, Node> associatedExplanations;
	private Node entireLF;
	private Map<String, Symbol> groupsOfEqualities;
	private Map<String,Set<Object>> eventToLiteral;
	private List<Node> observations;
	private Set<String> allExplanationNodes;
	private boolean forNLG3=false;
	private NLG2Lexicon lex=null;
	private int debug=2;
	private PrintStream out=null;

	public COPANLG2Generator(NLG2Data nlg2data,boolean forNLG3,NLG2Lexicon lex,int debug,PrintStream out) {
		independentLiteralsOrder=nlg2data.clauses;
		entireLF=nlg2data.logicForm;
		groupsOfEqualities=nlg2data.equalities;
		associatedExplanations=nlg2data.explanations;
		eventToLiteral=nlg2data.eventToLiteral;
		observations=nlg2data.obs;
		this.forNLG3=forNLG3;
		this.lex=lex;
		this.debug=debug;
		this.out=out;
		allExplanationNodes=new HashSet<String>();
		for(Node node:observations) {
			Node obE=COPANLG2.getRawExplanationSubgraphFor(node);
			COPANLG2.getPredicationsInExplanationChain(obE, allExplanationNodes);
		}
	}

	/**
	 * all Predications in independentLiteralsOrder should be clauses.
	 *  for each predication p in independentLiteralsOrder
	 *   build the standard S
	 * @throws Exception 
	 */
	public List<POS> generateSyntax() throws Exception {
		List<POS> ret=null;
		if (independentLiteralsOrder!=null) {
			Sentence s=null;
			for(Predication p:independentLiteralsOrder) {
				String reference=p.getEventualityName();
				s=(Sentence) createPOS(reference, null);

				if (ret==null) ret=new ArrayList<POS>();
				POS ex=null;
				Node explanation=null;
				if (associatedExplanations!=null && (explanation=associatedExplanations.get(p))!=null) {
					ex=generateExplanation(explanation);
				}
				if (ex!=null) {
					Coordination ns = new Coordination();
					ns.add(s);
					ns.add(ex);
					ns.setFunction("because");
					ret.add(ns);
				} else {
					ret.add(s);
				}
			}
		}
		return ret;
	}
	public POS createPOS(String reference, Node explanationChain) throws Exception {
		POS ret=null;
		if (eventToLiteral!=null && eventToLiteral.containsKey(reference)) {
			Object referenceNLU=disambiguate(eventToLiteral.get(reference),explanationChain,observations);
			String pname=NLUUtils.getPredicateName(referenceNLU);

			if (NLUUtils.isNegation(referenceNLU)) {
				ret=createPOS(NLUUtils.toString(NLUUtils.getArgument(referenceNLU, 0)), explanationChain);
				if (ret==null || !(ret instanceof Sentence) || ((Sentence)ret).getVerbPhrase()==null) System.err.println("couldn't apply negation to result: "+NLUUtils.toString(referenceNLU));
				else ((VP)((Sentence)ret).getVerbPhrase()).setNegated(true);
			} else if (NLUUtils.isVerb(pname, lex)) {
				Predication p = new Predication(referenceNLU,lex);
				String subjectReference=p.getSubject();
				POS np=createNP(subjectReference, explanationChain);
				VP vp=createVP(p, explanationChain);
				ret=new Sentence();
				((Sentence)ret).addSubject(np);
				((Sentence)ret).addVerbPhrase(vp);
			} else if (NLUUtils.isAdverb(pname, lex)) {
				ret=createPOS(NLUUtils.toString(NLUUtils.getArgument(referenceNLU, 0)), explanationChain);
				if (ret!=null) {
					if (ret instanceof VP) {
						((VP)ret).addModifier(normalizeText(pname, lex));
					} else if (ret instanceof NP) {
						((NP)ret).addModifier(normalizeText(pname, lex));
					} else if (ret instanceof Sentence) {
						((Sentence)ret).addVerbModifier(normalizeText(pname, lex));
					} else {
						System.err.println("ignoring modifier '"+pname+"' as returned argument is neither a verb nor a noun.");
					}
				}
			} else if (NLUUtils.isAdjective(pname, lex)) {
				/** 
				 * use a linking verb (e.g. "to be") to link the subject to the adjective
				 */
				Predication p=new Predication(referenceNLU,lex);
				if (p!=null) {
					String subjectReference=p.getSubject()!=null?p.getSubject():p.getObject();
					POS np=createNP(subjectReference,explanationChain);
					ret=new Sentence();
					((Sentence)ret).addSubject(np);
					VP vp=new VP();
					vp.setVerb(normalizeText(pname, lex));
					vp.setAdjectivePredicate(true);
					((Sentence)ret).addVerbPhrase(vp);
				}
			} else if (NLUUtils.isPP(pname,lex)) {
				POS content=createPOS(NLUUtils.toString(NLUUtils.getArgument(referenceNLU, 0)), explanationChain);
				ret=new PP();
				((PP)ret).setPreposition(normalizeText(pname, lex));
				((PP)ret).setComplement(content);
			}
		} else {
			ret=createNP(reference, explanationChain);
		}
		return ret;
	}

	/**
	 * get the first node of explanation
	 * 	get its parents
	 *   is it one parent? generate sentence for it (sentence x, pass it one to a further call to this function with explanation the parent)
	 *   is it more than 1 parent
	 *    generate a coordination (and) between the output of this function called on the n parents)
	 * @param explanation
	 * @param entireLF
	 * @param groupsOfEqualities
	 * @param lex
	 * @param eventToLiteral
	 * @return
	 * @throws Exception 
	 */
	public POS generateExplanation(Node explanation) throws Exception {
		if (explanation!=null && explanation.hasParents()) {
			Collection<Node> parents = null;
			try {
				parents=explanation.getParents();
			} catch (Exception e) {e.printStackTrace();}
			if (parents!=null && !parents.isEmpty()) {
				POS s=null;
				int mode=0;
				for (Node p:parents) {
					POS thisParent=generatePOSForExplanationNode(p);
					POS ancestorsOfThisParent=generateExplanation(p);
					if (thisParent!=null || ancestorsOfThisParent!=null) {
						POS entireParent=null;
						if (thisParent!=null) {
							entireParent=thisParent;
						}
						if (ancestorsOfThisParent!=null) {
							if (entireParent==null) entireParent=ancestorsOfThisParent;
							else {
								Sentence olds=(Sentence)entireParent;
								entireParent=new Coordination();
								((Coordination)entireParent).add(olds);
								((Coordination)entireParent).add(ancestorsOfThisParent);
								((Coordination)entireParent).setFunction("because");
							}
						}
						switch (mode) {
						case 0:
							s=entireParent;
							mode++;
							break;
						case 1:
							POS olds=s;
							s=new Coordination();
							((Coordination)s).add(olds);
							((Coordination)s).add(entireParent);
							mode++;
							break;
						default:
							((Coordination)s).add(entireParent);
							break;
						}
					}
				}
				return s;
			}
		}
		return null;
	}
	private POS generatePOSForExplanationNode(Node v) throws Exception {
		if (debug>1) out.println(" generating explanation using node: "+v.getName());
		Object nlu=NLUUtils.parse(v.getName(), false, false);
		String reference=NLUUtils.getEventualityName(nlu);
		return createPOS(reference, v);
	}

	/**
	 *  get type of thing
	 *   if reference is an eventuality, get the literal that defines it
	 *    get the literal's pos
	 *     generate accordingly
	 *   if reference is a variable find a monadic predicate that uses it as argument.
	 *    get the literal's pos
	 *     -case 1: NN-ADJs => it's a noun.
	 * @param reference
	 * @param entireLF
	 * @param groupsOfEqualities
	 * @return
	 */
	private POS createThing(String reference, Node entireLF, Map<String, Symbol> groupsOfEqualities) {

		return null;
	}

	/**
	 * if the input is a constant, just build an NP of that constant with a definite article.
	 * if the input is a variable, decide which literals to use to describe it and realise them
	 *  -case 1: there is 1 NN literal and several ADJ literals that define that variable. Build a modified NP.
	 * @param reference
	 * @param entireLF 
	 * @param groupsOfEqualities 
	 * @param eventToLiteral 
	 * @return
	 * @throws Exception 
	 */
	private POS createNP(String reference, Node explanationChain) throws Exception {
		POS ret=null;
		if (NLUUtils.isConstant(reference)) {
			ret=new NP(reference);
			((NP)ret).setDeterminer(DT.THE);
			return ret;
		} else {
			Map<String,Set<WFF>> literalsForMembersOfEqualityGroup=null;
			Set<String> eqReferences = NLUUtils.getAllVariableNamesEqualTo(groupsOfEqualities, reference);
			// it's a variable
			/**
			 * case 1: find if there are types defining this variable
			 *  if there is one, find if there are adjectives modifying this noun.
			 *   build NP
			 *  if there are more than 1 (considering equalities), apply the above procedure to each, build a conjunction
			 */
			boolean case1=false;
			Set<WFF> types = NLUGraphUtils.findAllMonadicPredications(entireLF);
			if (types!=null) {
				for(String eqRef:eqReferences) {
					Node referenceNode=entireLF.getNodeNamed(eqRef);
					try {
						Collection<Node> parents = referenceNode.getParents();
						//find if there is a NN parent that could be used to describe the reference.
						if (parents!=null) {
							for(Node p:parents) {
								if (NLUGraphUtils.isMonadicPredicate(p)) {
									WFF pwff=(WFF)p;
									Object nlu=pwff.getParsedNLUObject(false);
									String pname=NLUUtils.getPredicateName(nlu);
									if (NLUUtils.isNoun(pname, lex)) {
										if (literalsForMembersOfEqualityGroup==null) literalsForMembersOfEqualityGroup=new HashMap<String, Set<WFF>>();
										Set<WFF> literals=literalsForMembersOfEqualityGroup.get(eqRef);
										if (literals==null) literalsForMembersOfEqualityGroup.put(eqRef,literals=new HashSet<WFF>());
										literals.add(pwff);
										Set<WFF> mods=NLUGraphUtils.getModifiersFor(pwff,lex);
										filterModifiersWithExplanationChain(mods,allExplanationNodes);
										if (mods!=null) {
											Set<String> obs=NLG2Data.getEventualityThatareObservations(observations);
											if (obs==null) literals.addAll(mods);
											else {
												for(WFF m:mods) {
													Object mNlu=m.getParsedNLUObject(false);
													String ev=NLUUtils.getEventualityName(mNlu);
													if (obs.contains(ev))
														literals.add(m);
												}
											}
										}
										break;
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (literalsForMembersOfEqualityGroup!=null) {
					for(String vName:literalsForMembersOfEqualityGroup.keySet()) {
						Set<WFF> literals=literalsForMembersOfEqualityGroup.get(vName);
						case1=true;
						NP nn=generateThisNN_ADJsSet(literals,lex);
						if (ret==null) ret=nn;
						else if (ret instanceof NP) {
							NP oldnp=(NP)ret;
							ret=new Coordination();
							((Coordination)ret).add(oldnp);
							((Coordination)ret).add(nn);
						} else {
							((Coordination)ret).add(nn);
						}
					}
				}
			}
			/**
			 * case 2: it's a reference to an eventuality. find the literal defining that eventuality and verbalize it.
			 */
			if (!case1) {
				if (eventToLiteral!=null && eventToLiteral.containsKey(reference)) {
					ret=createPOS(reference, explanationChain);
				}
			}
		}
		return ret;
	}

	private void filterModifiersWithExplanationChain(Set<WFF> mods,Set<String> allExplanationNodes) {
		if (mods!=null) {
			Iterator<WFF> it=mods.iterator();
			while(it.hasNext()) {
				WFF m=it.next();
				if (allExplanationNodes.contains(m.getName())) {
					System.err.println("Removing this modifier: "+m+" because it's in the not an observation.");
					it.remove();
				}
			}
		}
	}
	
	private NP generateThisNN_ADJsSet(Set<WFF> literals, NLG2Lexicon lex) {
		NP ret=null;
		if (literals!=null) {
			for(WFF ln:literals) {
				Object l=ln.getParsedNLUObject(false);
				String pname=NLUUtils.getPredicateName(l);
				if (ret==null) ret=new NP(null);
				if (NLUUtils.isNoun(pname, lex)) {
					ret.setNoun(normalizeText(pname,lex));
				} else {
					ret.addModifier(normalizeText(pname,lex));
				}
			}
		}
		return ret;
	}

	private VP createVP(Predication p,Node explanationChain) throws Exception {
		String pname=p.getPredicate();
		VP vp=new VP();
		vp.setVerb(normalizeText(pname, lex));
		vp.setPassive(lex.isPassive(pname));
		//String ev=p.getEventualityname();
		//List<Object> mods=NLUUtils.getModifiersFor(ev,"VB",entireLF);
		int numArguments=p.getLength();
		for(int argPos=1;argPos<numArguments;argPos++) {
			//ARGTYPE type=lex.getTypeOfArgumentAtPosition(pname, argPos);
			String arg = p.getArgument(argPos);
			if (arg!=null) {
				POS a=createPOS(arg,explanationChain);
				vp.addArgument(a);
			}
		}
		return vp;
	}

	private List<Predication> getParentChainOfVerbs(Node explanation, NLG2Lexicon lex) throws Exception {
		List<Predication> ret=null;
		Node v=explanation;
		while((v=v.getSingleParent())!=null) {
			Object nlu=NLUUtils.parse(v.getName(), false, false);
			String pname=NLUUtils.getPredicateName(nlu);
			if (NLUUtils.isVerb(pname, lex)) {
				Predication p=new Predication(nlu,lex);
				if (p!=null) {
					if (ret==null) ret=new ArrayList<Predication>();
					ret.add(p);
				}
			} else if (lex.contains(pname) && lex.getType(pname)==null) System.err.println("POS info not available for lexicon item: "+pname);
		}
		return ret;
	}

	private boolean isThisNN_ADJsSet(Set<Object> literals, NLG2Lexicon lex) {
		if (literals!=null) {
			boolean foundNoun=false;
			for(Object l:literals) {
				String pname=NLUUtils.getPredicateName(l);
				if (NLUUtils.isNoun(pname, lex) && !foundNoun) foundNoun=true;
				else if (!NLUUtils.isAdjective(pname, lex)) return false;
			}
			return true;
		}
		return false;
	}
	
	private String normalizeText(String pname, NLG2Lexicon lex) {
		pname=NLUUtils.getName(pname);
		String ret=null;
		if (!forNLG3 && lex!=null) {
			List<String> surface=lex.getSurface(pname);
			if(surface!=null && !surface.isEmpty()) ret=surface.get(0);
		}
		if (ret==null) ret=pname;
		if (ret.endsWith("'")) {
			if (!forNLG3) ret=ret.substring(0, ret.length()-1);
		} else if (forNLG3) {
			ret=ret+"'";
		}
		return ret;
	}

	/**
	 * in case there are multiple literals that have the same eventuality (i.e. size of set>1) this code finds the one intended by finding the closest to the explanation chain of interest.
	 * @param set
	 * @param explanationChain
	 * @param observations
	 * @return
	 */
	private Object disambiguate(Set<Object> set, Node explanationChain, List<Node> observations) {
		if (set!=null) {
			if (set.size()>1) {
				if (explanationChain!=null) {
					Object found=searchExactMatch(set,explanationChain);
					if (found!=null) return found;
					found=searchClosestMatch(set,explanationChain,observations);
					if (found!=null) return found;
					try {
						String options=FunctionalLibrary.printCollection(FunctionalLibrary.map(set, NLUUtils.class.getMethod("toString", Object.class)), "", "", ",");
						System.err.println("multiple options for explanation chain of: "+explanationChain.getName()+": "+options);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
					try {
						String options=FunctionalLibrary.printCollection(FunctionalLibrary.map(set, NLUUtils.class.getMethod("toString", Object.class)), "", "", ",");
						System.err.println("multiple options for null explanation chain: "+options);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			else return set.iterator().next();
		}
		return set;
	}

	private Object searchClosestMatch(Set<Object> set,Node explanationChain, List<Node> observations) {
		Object found=null;
		int bestDistance=0;
		if (observations!=null) {
			String exName=explanationChain.getName();
			for(Object o:set) {
				String name=NLUUtils.toString(o);
				for(Node n:observations) {
					Node nn=n.getNodeNamed(name,true);
					if (nn!=null) {
						Node exNode=n.getNodeNamed(exName, true);
						if (exNode!=null) {
							int distance=exNode.getDistanceTo(exNode,false);
							if (found==null || bestDistance>distance) {
								found=o;
								bestDistance=distance;
							}
						}
					}
				}
			}
		}
		return found;
	}
	private Object searchExactMatch(Set<Object> set, Node explanationChain) {
		Object found=null;
		for(Object o:set) {
			String name=NLUUtils.toString(o);
			if (explanationChain.getNodeNamed(name,true)!=null) {
				if (found==null) found=o;
				else {
					found=null;
					break; 
				}
			}
		}
		return found;
	}

}
