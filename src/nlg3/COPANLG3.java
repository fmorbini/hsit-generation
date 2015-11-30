package nlg3;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Node;
import lf.NLUUtils;
import lf.Predication;
import lf.pos.ADJ;
import lf.pos.Coordination;
import lf.pos.NP;
import lf.pos.POS;
import lf.pos.PP;
import lf.pos.Sentence;
import lf.pos.VP;
import nlg2.COPANLG2;
import nlg2.COPANLG2Generator;
import nlg2.NLG2Data;
import nlg3.selection.ParserSelection;
import nlg3.selection.SelectionI;
import simplenlg.features.Feature;
import simplenlg.features.InternalFeature;
import simplenlg.framework.DocumentElement;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.english.Realiser;

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
public class COPANLG3 {


	private COPANLG2 nlg2;
	private Rules rules;
	public int debug=0;
	private NLG3Lexicon lex;
	private PrintStream out;
	private SelectionI selectionMethod=null;

	public COPANLG3(String nlg2Lexicon,String rulesfile,int debug,PrintStream out,SelectionI s,boolean replaceWithNames) {
		NLUUtils.replaceWithNames=replaceWithNames;
		if (out==null) out=System.out;
		this.out=out;
		this.lex=new NLG3Lexicon(nlg2Lexicon);
		this.nlg2 = new COPANLG2(this.lex,out,replaceWithNames);
		this.debug=debug;
		this.nlg2.setDebug(debug-1);
		this.nlg2.setForNLG3(true);
		this.rules=new Rules(rulesfile,debug);
		nlg2.getRealizer().getRealiser().setCommaSepPremodifiers(false);
		this.selectionMethod=s;
	}
	
	public List<String> getCandidates(File proof, int optionToPrint) throws Exception {
		NLG2Data nlg2data=nlg2.getSyntaxPrecursor(proof);
		List<Predication> independentLiteralsOrder=nlg2data.clauses;
		Map<Predication, Node> associatedExplanations=nlg2data.explanations;
		COPANLG2Generator generator = new COPANLG2Generator(nlg2data, nlg2.getForNLG3(), nlg2.getLexicon(),debug,out);
 
		Coordination root=new Coordination();
		root.setAsList();
		if (independentLiteralsOrder!=null) {
			filterObservationWithExplanations(independentLiteralsOrder,associatedExplanations);
			for(Predication p:independentLiteralsOrder) {
				String reference=p.getEventualityName();
				Object pNLU=p.getSource();
				Node pNode=new Node(NLUUtils.toString(pNLU));
				if (debug>1) out.println("generating sentence for reference: "+reference+" in predication: "+p);
				Sentence s=(Sentence) generator.createPOS(reference,pNode);
				if (debug>0) out.println(s);
				POS ex=null;
				Node explanation=null;
				if (associatedExplanations!=null && (explanation=associatedExplanations.get(p))!=null) {
					if (debug>1) out.println(" explanation: "+explanation.getName());
					ex=generator.generateExplanation(explanation);
				}
				if (ex!=null) {
			        Coordination ns = new Coordination();
			        ns.add(s);
			        ns.add(ex);
			        ns.setFunction("because");
			        root.add(ns);
				} else {
					root.add(s);
				}
			}
		}
		
		if (debug>0) out.println(root);
		List<POSwT> options = rules.generateSyntax(root,lex);
		List<String> ret=null;
		int oC=1;
		for(POSwT oNonLexicalH:options) {
			POS oNonLexical=oNonLexicalH.getLast();
			List<POS> os=generateLexicalVariants(oNonLexical,lex);
			for(POS o:os) {
				if (oC==optionToPrint) {
					out.println(oC+":\n"+oNonLexicalH);
				}
				if (ret==null) ret=new ArrayList<String>();
				/*
				if (oC==812) {
					for(int i=0;i<1000;i++) {
						String r=nlg2.processSyntax(o);
						System.out.println(r);
					}
				}
				*/
				/*
				try {
					o=o.getElementAt(Arrays.asList(new Integer[]{0, 1}));
				} catch (Exception e) {}
				*/
				String r=nlg2.processSyntax(o);
				r=StringUtils.removeLeadingAndTrailingSpaces(r);
				if (debug>0) out.println(oC+": "+r);
				ret.add(r);
				oC++;
			}
		}
		out.println((oC-1)+" options.");
		return ret;
	}
	
	private void filterObservationWithExplanations(List<Predication> independentLiteralsOrder,Map<Predication, Node> associatedExplanations) {
		if (associatedExplanations!=null) {
			Set<String> exs=null;
			for(Predication p:independentLiteralsOrder) {
				Node explanation = associatedExplanations.get(p);
				HashSet<String> explanations = new HashSet<String>();
				nlg2.getPredicationsInExplanationChain(explanation, explanations);
				if (explanations!=null) {
					for(String e:explanations) {
						Predication ep=new Predication(NLUUtils.parse(e, false, false));
						if (exs==null) exs=new HashSet<String>();
						exs.add(ep.toString());
					}
				}
			}
			if (exs!=null) {
				Iterator<Predication> it=independentLiteralsOrder.iterator();
				while(it.hasNext()) {
					Predication p=it.next();
					if (exs.contains(p.toString())) {
						if (debug>0) out.println("removing independent literal: "+p+" because it's used in an explanation chain.");
						it.remove();
					}
				}
			}
		}
	}

	private class StringWithPOS{
		public StringWithPOS(String s) {
			this.s=s;
		}
		public StringWithPOS(String s, String pos) {
			this(s);
			this.pos=pos;
		}
		String s;
		String pos;
		
		@Override
		public String toString() {
			return s+"-"+pos;
		}
	}
	private List<POS> generateLexicalVariants(POS o,NLG3Lexicon lex) {
		List<POS> ret=null;
		List<StringWithPOS> primed=getAllPrimedStrings(o);
		if (primed==null || primed.isEmpty()) {
			if (ret==null) ret=new ArrayList<POS>();
			ret.add(o);
		} else {
			Map<String,Set<String>> realizations=new HashMap<String, Set<String>>();
			for(StringWithPOS pp:primed) {
				String pos=pp.pos;
				String p=pp.s;
				Set<String> pRealizations=realizations.get(p);
				if (pRealizations==null) realizations.put(p, pRealizations=new LinkedHashSet<String>());
				List<String> surfaces=(StringUtils.isEmptyString(pos))?lex.getSurface(p):lex.getSurface(p,pos);
				if (surfaces==null) {
					if (p.endsWith("'")) p=p.substring(0, p.length()-1);
					pRealizations.add(p);
				}
				else for(String surface:surfaces) pRealizations.add(surface);
			}
			if (ret==null) ret=new ArrayList<POS>();
			apply(o,null,primed, realizations,ret);
		}
		return ret;
	}

	private void apply(POS o,List<String> substitutions,List<StringWithPOS> primed, Map<String, Set<String>> realizations,List<POS> ret) {
		int i=substitutions==null?0:substitutions.size();
		if (i>=primed.size()) {
			POS no=doSub(o,substitutions);
			ret.add(no);
		} else {
			StringWithPOS ppp=primed.get(i);
			String pp=ppp.s;
			i++;
			Set<String> ppRealizations = realizations.get(pp);
			boolean single=(ppRealizations.size()==1);
			for(String r:ppRealizations) {
				List<String> nSubstitutions=null;
				if (single) nSubstitutions=(substitutions!=null)?substitutions:new ArrayList<String>();
				else nSubstitutions=(substitutions!=null)?new ArrayList<String>(substitutions):new ArrayList<String>();
				nSubstitutions.add(r);
				apply(o,nSubstitutions,primed,realizations,ret);
			}
		}
	}
	private POS doSub(POS oo, List<String> substitutions) {
		int i=0;
		if (oo!=null) {
			POS o=oo.clone();
			Deque<POS> q=new LinkedList<POS>();
			q.add(o);
			while(!q.isEmpty()) {
				POS t=q.pop();
				if (t!=null) {
					if (t instanceof VP) {
						if (endsWithPrime(((VP) t).getVerb())) { 
							((VP) t).setVerb(substitutions.get(i));
							i++;
						}
						if (((VP) t).mods!=null) {
							for(int j=0;j<((VP) t).mods.size();j++) {
								String m=((VP) t).mods.get(j);
								if (endsWithPrime(m)) {
									((VP) t).mods.set(j, substitutions.get(i));
									i++;
								}
							}
						}
					} else if (t instanceof NP) {
						if (endsWithPrime(((NP) t).getNoun())) {
							((NP) t).setNoun(substitutions.get(i));
							i++;
						}
					} else if (t instanceof PP) {
						if (endsWithPrime(((PP)t).getPreposition())) {
							((PP) t).setPreposition(substitutions.get(i));
							i++;
						}
					} else if (t instanceof Coordination) {
						((Coordination) t).setFunction(substitutions.get(i));
						i++;
					} else if (t instanceof ADJ) {
						if (endsWithPrime(t.toString())) {
							((ADJ)t).set(substitutions.get(i));
							i++;
						}
					}
					List<POS> cs = t.getChildren();
					if (cs!=null) q.addAll(cs);
				}
			}
			return o;
		}
		return null;
	}

	private List<StringWithPOS> getAllPrimedStrings(POS o) {
		List<StringWithPOS> ret=null;
		if (o!=null) {
			Deque<POS> q=new LinkedList<POS>();
			q.add(o);
			while(!q.isEmpty()) {
				POS t=q.pop();
				if (t!=null) {
					if (t instanceof VP) {
						if (endsWithPrime(((VP) t).getVerb())) { 
							if (ret==null) ret=new ArrayList<StringWithPOS>();
							ret.add(new StringWithPOS(((VP) t).getVerb(),((VP) t).isAdjectivePredicate()?NLUUtils.AdjPOS:NLUUtils.VerbPOS));
						}
						if (((VP) t).mods!=null) {
							for(String m:((VP) t).mods) {
								if (endsWithPrime(m)) {
									if (ret==null) ret=new ArrayList<StringWithPOS>();
									ret.add(new StringWithPOS(m));
								}
							}
						}
					} else if (t instanceof NP) {
						if (endsWithPrime(((NP) t).getNoun())) {
							if (ret==null) ret=new ArrayList<StringWithPOS>();
							ret.add(new StringWithPOS(((NP) t).getNoun(),NLUUtils.NounPOS));
						}
					} else if (t instanceof PP) {
						if (endsWithPrime(((PP) t).getPreposition())) {
							if (ret==null) ret=new ArrayList<StringWithPOS>();
							ret.add(new StringWithPOS(((PP) t).getPreposition(),NLUUtils.PPPOS));
						}
					} else if (t instanceof Coordination) {
						if (ret==null) ret=new ArrayList<StringWithPOS>();
						ret.add(new StringWithPOS(((Coordination) t).getFunction().toString(),NLUUtils.PPPOS));
					} else if (t instanceof ADJ) {
						if (endsWithPrime(((ADJ)t).toString())) {
							if (ret==null) ret=new ArrayList<StringWithPOS>();
							ret.add(new StringWithPOS(t.toString()));
						}
					}

					List<POS> cs = t.getChildren();
					if (cs!=null) q.addAll(cs);
				}
			}
		}
		return ret;
	}

	private boolean endsWithPrime(String m) {
		if (!StringUtils.isEmptyString(m)) return m.endsWith("'");
		return false;
	}

	public void runThisRange(int onlyThis) throws IOException {
		runThisRange(onlyThis, -1);
	}
	public void runThisRange(int onlyThis, int optionToPrint) throws IOException {
		runThisRange(onlyThis, onlyThis, optionToPrint);
	}
	public void runThisRange(int start,int end, int optionToPrint) throws IOException {
		long startTime=System.currentTimeMillis();
		for(int i=start;i<=end;i++) {
			File prb=new File("paetc/new-graphs-2015-09-22/prb-"+i+"-proof.dot");
			//File prb=new File("paetc/new-graphs-2015-09-16/prb-"+i+"-proof.dot");
			//File prb=new File("paetc/new-graphs/prb-"+i+"-proof.dot");
			//File prb=new File("paetc/prbs/prb-"+i+"-proof.dot");
			try {
				if (out!=System.out) System.out.println(i);
				out.print(i+"\n");
				List<String> candidates = getCandidates(prb,optionToPrint);
				
				String r=selectionMethod.selectBest(candidates,optionToPrint);
				
				out.print("input: "+((candidates!=null && !candidates.isEmpty())?candidates.get(0):"null")+"\n");
				out.print("output: "+r+"\n");
				out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		out.close();
		System.out.println("finished in "+((System.currentTimeMillis()-startTime)/1000)+" seconds.");
	}
	
	public static void test() {
		Lexicon lexicon = Lexicon.getDefaultLexicon();
		NLGFactory nlgFactory = new NLGFactory(lexicon);
		Realiser realiser = new Realiser(lexicon);
		SPhraseSpec s=nlgFactory.createClause();
		NPPhraseSpec subject=nlgFactory.createNounPhrase("circle");
		subject.setDeterminer("the");
		s.setSubject(subject);
		VPPhraseSpec vpnlg = nlgFactory.createVerbPhrase("be");
		NPPhraseSpec complement=nlgFactory.createNounPhrase("parent");
		NLGElement pr = nlgFactory.createWord("him",LexicalCategory.PRONOUN);
		pr.setFeature(Feature.POSSESSIVE, true);
		complement.setFeature(InternalFeature.SPECIFIER, pr);
		vpnlg.setComplement(complement);
		s.setVerbPhrase(vpnlg);
		DocumentElement d = nlgFactory.createSentence(s);
		String r=realiser.realise(d).getRealisation();

		System.out.println(r);
	}
	
	private static void runAll(String filename) throws Exception {
		PrintStream out=filename==null?System.out:new PrintStream(new File(filename));
		int debug=3;
		//COPANLG3 nlg = new COPANLG3("predicateList.xlsx","rules-nlg3.xlsx",debug,out,new ParserSelection(out,debug,"http://localhost:8081",new File("plog-parse-normalized.txt"),-14.61,35.74),false);
		//COPANLG3 nlg = new COPANLG3("predicateList.xlsx","rules-nlg3.xlsx",debug,out,new ParserSelection(out,debug,"http://localhost:8081",new File("plog-parse-normalized-norep.txt"),-10.68,106.59),false);
		COPANLG3 nlg = new COPANLG3("predicateList.xlsx","rules-nlg3.xlsx",debug,out,new ParserSelection(out,debug,"http://localhost:8081",new File("plog-plen-rep.txt"),null,null),false);
		nlg.runThisRange(1, 100,-1);
	}

	private static void runOne(int which) throws Exception {
		runOne(which, null);
	}
	private static void runOne(int which,Integer optionToPrint) throws Exception {
		PrintStream out=System.out;int debug=1;
		COPANLG3 nlg = new COPANLG3("predicateList.xlsx","rules-nlg3.xlsx",debug,out,new ParserSelection(out,debug,"http://localhost:8081",null,null),false);
		if (optionToPrint!=null) nlg.runThisRange(which,optionToPrint);
		else nlg.runThisRange(which);
	}

	public static void main(String[] args) throws Exception {
		//test();
		//runAll("log-nlg3-simple-9d888d49022d5fe04add6f22656b220edbd4b0b6-graphs-2015-09-22-plen-rep.txt");
		runOne(1);
		/*
		PrintStream out=new PrintStream(new File("log-nlg3-simple.txt"));int debug=0;
		PrintStream out=new PrintStream(new File("log-nlg3-all.txt"));int debug=1;
		PrintStream out=System.out;int debug=1;
		COPANLG3 nlg = new COPANLG3("predicateList.xlsx","rules-nlg3.xlsx",debug,out,new ParserSelection(out,debug,"http://localhost:8081"));
		nlg.runThisRange(1, 100,-1);
		// to try 89, 74
		nlg.runThisRange(17);
		*/
	}

}
