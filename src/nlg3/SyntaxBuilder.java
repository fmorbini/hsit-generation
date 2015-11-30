package nlg3;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lf.pos.Coordination;
import lf.pos.NP;
import lf.pos.POS;
import lf.pos.POSI.DT;
import lf.pos.PP;
import lf.pos.Sentence;
import lf.pos.VP;
import nlg3.properties.Properties;
import nlg3.properties.Property;
import nlg3.reference.ReferenceItem;

public class SyntaxBuilder {
	public enum NODE_TO_CHANGE {THIS,PARENT};
	public enum TYPE {MK_SENTENCE,MK_NP,MK_VP,MK_PP,MK_CC,MK_PR,SUBJECT_OF,VP_OF,VERB_OF,INFINITIVE_OF,PROGRESSIVE_OF,OBJECT_OF,THIS,CNST,NEGATED,TYPE_OF,
		REFERENCES,LENGTH,FIRST,GET,CCS_OF,CONJ_OF,CURRENT_POSITION,NEXT_POSITION,NEXTNEXT_POSITION,PARENT_OF,CHANGE_PARENT,SUBLIST,CHILDREN_OF,ADD_MOD,NOUN,SETPADJ,ISPADJ,NEGATED_VP,CONTAINS,
		POSSIBLEREFS,HAS_PROPERTY,IS_PR,IS_RF,EVERY,MAP,ADD_COMPLEMENT,GET_COMPLEMENTS,RESET_COMPLEMENTS,AND,OR,NOT,GT,GE,LT,LE,EQ,REFERTO,PRINT,POSSESSIVE_OF,IS_IN_OBJECT_POSITION,
		CLONE
		};//TOPPMATCH,
	private TYPE operation;
	private String cnst;
	private List<SyntaxBuilder> args;
	private NODE_TO_CHANGE nodeToChange=NODE_TO_CHANGE.THIS;
	private int row=-1,column=-1;
	private int group=-1;
	private Set<Integer> disabledBuilders=null;
	
	private static final Collator myCollator = Collator.getInstance();
	static {
		myCollator.setStrength(Collator.PRIMARY); //ignores case
	}

	public static SyntaxBuilder create(String fname,List<SyntaxBuilder> args) throws Exception {
		SyntaxBuilder ret=null;
		if (fname.equalsIgnoreCase("change_parent")) {
			ret=args.get(0);
			ret.nodeToChange=NODE_TO_CHANGE.PARENT;
		} else {
			ret=new SyntaxBuilder(fname, args);
		}
		return ret;
	}
	
	public void setGroup(int group) {
		this.group = group;
	}
	public boolean isInGroup(int group) {
		return (this.group<0 || group<0 || this.group==group);
	}
	
	private SyntaxBuilder(String fname,List<SyntaxBuilder> args) throws Exception {
		if (fname.equalsIgnoreCase("make_sentence")) this.operation=TYPE.MK_SENTENCE;
		else if (fname.equalsIgnoreCase("make_np")) this.operation=TYPE.MK_NP;
		else if (fname.equalsIgnoreCase("make_vp")) this.operation=TYPE.MK_VP;
		else if (fname.equalsIgnoreCase("make_pp")) this.operation=TYPE.MK_PP;
		else if (fname.equalsIgnoreCase("make_cc")) this.operation=TYPE.MK_CC;
		else if (fname.equalsIgnoreCase("make_pronoun")) this.operation=TYPE.MK_PR;
		else if (fname.equalsIgnoreCase("make_clone")) this.operation=TYPE.CLONE;
		else if (fname.equalsIgnoreCase("add_mod")) this.operation=TYPE.ADD_MOD;
		else if (fname.equalsIgnoreCase("subject")) this.operation=TYPE.SUBJECT_OF;
		else if (fname.equalsIgnoreCase("object")) this.operation=TYPE.OBJECT_OF;
		else if (fname.equalsIgnoreCase("noun")) this.operation=TYPE.NOUN;
		else if (fname.equalsIgnoreCase("vp")) this.operation=TYPE.VP_OF;
		else if (fname.equalsIgnoreCase("ccs")) this.operation=TYPE.CCS_OF;
		else if (fname.equalsIgnoreCase("conj")) this.operation=TYPE.CONJ_OF;
		else if (fname.equalsIgnoreCase("infinitive")) this.operation=TYPE.INFINITIVE_OF;
		else if (fname.equalsIgnoreCase("progressive")) this.operation=TYPE.PROGRESSIVE_OF;
		else if (fname.equalsIgnoreCase("possessive")) this.operation=TYPE.POSSESSIVE_OF;
		else if (fname.equalsIgnoreCase("children")) this.operation=TYPE.CHILDREN_OF;
		else if (fname.equalsIgnoreCase(".")) this.operation=TYPE.THIS;
		else if (fname.equalsIgnoreCase("verb")) this.operation=TYPE.VERB_OF;
		else if (fname.equalsIgnoreCase("negated")) this.operation=TYPE.NEGATED;
		else if (fname.equalsIgnoreCase("predicateAdjective")) this.operation=TYPE.SETPADJ;
		else if (fname.equalsIgnoreCase("type")) this.operation=TYPE.TYPE_OF;
		else if (fname.equalsIgnoreCase("refs")) this.operation=TYPE.REFERENCES;
		else if (fname.equalsIgnoreCase("len")) this.operation=TYPE.LENGTH;
		else if (fname.equalsIgnoreCase("get")) this.operation=TYPE.GET;
		else if (fname.equalsIgnoreCase("first")) this.operation=TYPE.FIRST;
		else if (fname.equalsIgnoreCase("parent_of")) this.operation=TYPE.PARENT_OF;
		else if (fname.equalsIgnoreCase("i")) this.operation=TYPE.CURRENT_POSITION;
		else if (fname.equalsIgnoreCase("i+1")) this.operation=TYPE.NEXT_POSITION;
		else if (fname.equalsIgnoreCase("i+2")) this.operation=TYPE.NEXTNEXT_POSITION;
		else if (fname.equalsIgnoreCase("sublist")) this.operation=TYPE.SUBLIST;
		//else if (fname.equalsIgnoreCase("topPMatch")) this.operation=TYPE.TOPPMATCH;
		else if (fname.equalsIgnoreCase("add_complement")) this.operation=TYPE.ADD_COMPLEMENT;
		else if (fname.equalsIgnoreCase("reset_complements")) this.operation=TYPE.RESET_COMPLEMENTS;
		else if (fname.equalsIgnoreCase("get_complements")) this.operation=TYPE.GET_COMPLEMENTS;
		//boolean extractors used by preconditions
		else if (fname.equalsIgnoreCase("isNegated")) this.operation=TYPE.NEGATED_VP;
		else if (fname.equalsIgnoreCase("isPredicateAdjective")) this.operation=TYPE.ISPADJ;
		else if (fname.equalsIgnoreCase("contains")) this.operation=TYPE.CONTAINS;
		else if (fname.equalsIgnoreCase("isPronoun")) this.operation=TYPE.IS_PR;
		else if (fname.equalsIgnoreCase("isReflexive")) this.operation=TYPE.IS_RF;
		else if (fname.equalsIgnoreCase("isInObjectPosition")) this.operation=TYPE.IS_IN_OBJECT_POSITION;
		else if (fname.equalsIgnoreCase("getPossibleReferences")) this.operation=TYPE.POSSIBLEREFS;
		else if (fname.equalsIgnoreCase("referTo")) this.operation=TYPE.REFERTO;
		else if (fname.equalsIgnoreCase("every")) this.operation=TYPE.EVERY;
		else if (fname.equalsIgnoreCase("map")) this.operation=TYPE.MAP;
		else if (fname.equalsIgnoreCase("^")) this.operation=TYPE.AND;
		else if (fname.equalsIgnoreCase("|")) this.operation=TYPE.OR;
		else if (fname.equalsIgnoreCase("not")) this.operation=TYPE.NOT;
		else if (fname.equalsIgnoreCase(">=")) this.operation=TYPE.GE;
		else if (fname.equalsIgnoreCase(">")) this.operation=TYPE.GT;
		else if (fname.equalsIgnoreCase("<=")) this.operation=TYPE.LE;
		else if (fname.equalsIgnoreCase("<")) this.operation=TYPE.LT;
		else if (fname.equalsIgnoreCase("==")) this.operation=TYPE.EQ;
		else if (fname.equalsIgnoreCase("print")) this.operation=TYPE.PRINT;
		//added by Emily 06.30
		else if (fname.equalsIgnoreCase("hasProperty")) this.operation=TYPE.HAS_PROPERTY;
		else {
			if (args==null) {
				this.operation=TYPE.CNST;
				this.cnst=fname;
			}
			else throw new Exception("unknown operation: "+fname);
		}
		this.args=args;
	}
	
	
	public void setRow(int row) {
		this.row = row;
	}
	public int getRow() {
		return row;
	}
	public void setColumn(int column) {
		this.column = column;
	}
	public int getColumn() {
		return column;
	}
	
	public boolean changeParent() {
		return nodeToChange==NODE_TO_CHANGE.PARENT;
	}
	
	public Object generateSyntaxRoot(POS p,TraversalState state,Map<String,Object> variables,boolean asTrigger) throws Exception {
		Object result = generateSyntax(p, state, variables, asTrigger);
		if (!asTrigger && result!=null && result instanceof POS && ((POS)result).isPronoun() && p instanceof NP) {
			POS reference = ((NP)result).getReference();
			if (reference!=null && reference instanceof NP) {
				Collection<ReferenceItem> rs=null;
				if (((POS)result).isReflexivePronoun()) {
					POS subject = getSubjectOfThisObject(state);
					rs = getPossibleReferencesFor((NP)subject,state);
					/*
					ReferenceStack references=state.getReferencesForState(state.coord);
					ReferenceItem found=null;
					if (references!=null) {
						for(ReferenceItem i:references) {
							if (i.getOriginal().equals(subject)) {
								found=i;
								break;
							}
						}
					}
					if (found!=null) {
						rs=new HashSet<ReferenceItem>();
						rs.add(found);
					} else {
						throw new Exception("failed to find reference item for subject of reflexive pronoun.");
					}
					*/
				} else {
					rs = getPossibleReferencesFor((NP)reference,state);
				}
				if (rs==null || rs.isEmpty()) {
					System.err.println("found no references after pronoun introduction: "+reference);
				} else {
					if (rs.size()>1) {
						System.err.println("found multiple references after pronoun introduction.");
						System.err.println(" reference object: "+reference);
						System.err.println(" matches:");
						for(ReferenceItem ri:rs) {
							System.err.println("  "+ri.getOriginal());
						}
					}
					Iterator<ReferenceItem> rsit=rs.iterator();
					while(rsit.hasNext()) {
						ReferenceItem r=rsit.next();
						if (r.hasUnavailableProperties()) {
							Set<String> ups=new HashSet<String>(r.getUnavailableProperties());
							for(String up:ups) r.setAsAvailable(up);
						}
					}
				}
			} else throw new Exception("pronoun has null reference.");
		}
		return result;
	}
	private Object generateSyntax(POS p,TraversalState state,Map<String,Object> variables,boolean asTrigger) throws Exception {
		POS ret=null;
		switch (operation) {
		case CNST:
			if (variables!=null && variables.containsKey(cnst)) return variables.get(cnst);
			else return cnst;
		case MK_SENTENCE:
			assert(args!=null && args.size()==2); //subject and vp
			ret=new Sentence();
			SyntaxBuilder subjectArg=args.get(0);
			if (subjectArg!=null) { 
				POS subject=(POS)subjectArg.generateSyntax(p,state,variables,asTrigger);
				((Sentence)ret).addSubject(subject);
			}
			SyntaxBuilder vpArg=args.get(1);
			if (vpArg!=null) { 
				POS vp=(POS)vpArg.generateSyntax(p,state,variables,asTrigger);
				((Sentence)ret).setVerbPhrase(vp);
			}
			break;
		case SUBJECT_OF:
			assert(args!=null && args.size()==1); //the thing from which to extract the subject
			SyntaxBuilder arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof Sentence) return ((Sentence)thing).getSubject();
			}
			break;
		case OBJECT_OF:
			assert(args!=null && args.size()==1); //the thing from which to extract the object
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof Sentence && ((Sentence) thing).getVerbPhrase() instanceof VP) return ((VP)((Sentence) thing).getVerbPhrase()).getObject();
				else if (thing instanceof VP) return ((VP) thing).getObject();
			}
			break;
		case THIS:
			return state.root.getLast().getElementAt(state.coord);
		case MK_NP:
			assert(args!=null && args.size()==1); //the String to become the subject 
			SyntaxBuilder npArg=args.get(0);
			if (npArg!=null) { 
				Object thing=npArg.generateSyntax(p,state,variables,asTrigger);
				if (thing!=null) {
					if (thing instanceof NP) return thing;
					else if (thing instanceof String) {
						NP r=new NP((String) thing);
						r.setDeterminer(DT.NULL);
						return r;
					}
					else System.err.println("asking a "+thing.getClass().getName()+" to be returned as a NP.");
				} 
			}
			break;
		case MK_VP:
			assert(args!=null && args.size()>=1); //the vrb plus its complements
			ret=new VP();
			if (p!=null) {
				if (p instanceof Sentence) {
					POS vp = ((Sentence) p).getVerbPhrase();
					if (vp instanceof VP) {
						ret=vp.clone();
					}
				} else if (p instanceof VP) {
					ret=p.clone();
				}
			}
			((VP)ret).getArguments().clear();
			SyntaxBuilder verbArg=args.get(0);
			if (verbArg!=null) { 
				Object verb=verbArg.generateSyntax(p,state,variables,asTrigger);
				((VP)ret).setVerb(verb.toString());
			}
			for(int i=1;i<args.size();i++) {
				SyntaxBuilder aArg=args.get(i);
				if (aArg!=null) { 
					POS a=(POS)aArg.generateSyntax(p,state,variables,asTrigger);
					((VP)ret).addArgument(a);
				}
			}
			break;
		case MK_PP:
			assert(args!=null && args.size()==2); //the preposition plus its argument 
			ret=new PP();
			SyntaxBuilder prepositionArg=args.get(0);
			if (prepositionArg!=null) { 
				Object preposition=prepositionArg.generateSyntax(p,state,variables,asTrigger);
				((PP)ret).setPreposition(preposition.toString());
			}
			SyntaxBuilder complementArg=args.get(1);
			if (complementArg!=null) { 
				POS complement=(POS)complementArg.generateSyntax(p,state,variables,asTrigger);
				((PP)ret).setComplement(complement);
			}
			break;
		case ADD_COMPLEMENT:
			assert(args!=null && args.size()==2); //the thing to which we want to add a complement 
			SyntaxBuilder sentenceArg=args.get(0);
			SyntaxBuilder ppArg=args.get(1);
			if (sentenceArg!=null && ppArg!=null) { 
				POS sentence=(POS)sentenceArg.generateSyntax(p,state,variables,asTrigger);
				Object pp=ppArg.generateSyntax(p,state,variables,asTrigger);
				if (sentence!=null && pp!=null && sentence instanceof POS) {
					sentence=sentence.clone();
					LinkedList<Object> ccs=new LinkedList<Object>();
					ccs.push(pp);
					while(!ccs.isEmpty()) {
						Object cc=ccs.pop();
						if (cc instanceof Collection) ccs.addAll((Collection)cc);
						else if (cc instanceof POS) addComplementTo((POS)cc,sentence);
						else throw new Exception("invalid complement returned.");
					}
				}
				return sentence;
			}
			break;
		case RESET_COMPLEMENTS:
			assert(args!=null && args.size()==1); //the thing for which we want to reset the complements 
			sentenceArg=args.get(0);
			if (sentenceArg!=null) { 
				Object sentence=sentenceArg.generateSyntax(p,state,variables,asTrigger);
				if (sentence!=null) {
					if (sentence instanceof Sentence) {
						sentence=((Sentence)sentence).clone();
						((Sentence)sentence).complements.clear();
						return sentence;
					} else if (sentence instanceof VP) {
						sentence=((VP)sentence).clone();
						((VP)sentence).getArguments().clear();
						return sentence;
					}
				}
			}
			break;
		case GET_COMPLEMENTS:
			assert(args!=null && args.size()==1); //the thing for which we want to return the complements 
			sentenceArg=args.get(0);
			if (sentenceArg!=null) { 
				Object sentence=sentenceArg.generateSyntax(p,state,variables,asTrigger);
				if (sentence!=null) {
					if (sentence instanceof Sentence) {
						sentence=((Sentence)sentence).clone();
						return ((Sentence)sentence).complements;
					} else if (sentence instanceof VP) {
						sentence=((VP)sentence).clone();
						return ((VP)sentence).getArguments();
					}
				}
			}
			break;
		case CLONE:
			assert(args!=null && args.size()==1); //the preposition plus its argument 
			SyntaxBuilder thingArg = args.get(0);
			if (thingArg!=null) { 
				Object thing=thingArg.generateSyntax(p,state,variables,asTrigger);
				if (thing!=null && thing instanceof POS) {
					return ((POS)thing).clone();
				}
			}
			break;
		case MK_PR:
			SyntaxBuilder ta=args.get(0); //the NP to be substituted by this pronoun.
			ret=null;
			if (ta!=null) { 
				POS t=(POS) ta.generateSyntax(p,state,variables,asTrigger);
				ret=getPronoun(t,state);
				if (ret!=null) {
					if (isReflexive(t,state)) {
						((NP) ret).setReflexive(true);
					}
				}
			}
			break;
		case INFINITIVE_OF:
			assert(args!=null && args.size()==1); //the thing (VP) to be made in infinitive form
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof VP) {
					VP newp;
					newp = (VP)thing.clone();
					newp.setInfinitive(true);
					return newp;
				}
			}
			break;
		case PROGRESSIVE_OF:
			assert(args!=null && args.size()==1); //the thing (VP) to be made in progressive form
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof VP) {
					VP newp;
					newp = (VP)thing.clone();
					newp.setProgressive(true);
					return newp;
				}
			}
			break;
		case POSSESSIVE_OF:
			assert(args!=null && args.size()==1); //the thing (NP) to be made in possessive form
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof NP) {
					NP newnp;
					newnp = (NP)thing.clone();
					newnp.setPossessiveForm(true);
					return newnp;
				}
			}
			break;
		case NEGATED:
			assert(args!=null && args.size()==1); //the thing (VP) to be made in infinitive form
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof VP) {
					VP newp;
					newp = ((VP)thing).clone();
					newp.setNegated(!((VP)thing).isNegated());
					return newp;
				}
			}
			break;
		case SETPADJ:
			assert(args!=null && args.size()==1); //the thing (VP) to be made in predicate adjective mode
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof VP) {
					VP newp;
					newp = ((VP)thing).clone();
					newp.setAdjectivePredicate(true);
					return newp;
				}
			}
			break;
		case VP_OF:
			assert(args!=null && args.size()==1); //the sentence from which to get the VP
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof Sentence) return ((Sentence) thing).getVerbPhrase();
			}
			break;
		case NOUN:
			assert(args!=null && args.size()==1); //the NP froum which to get the noun
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof NP) return ((NP) thing).getNoun();
			}
			break;
		case VERB_OF:
			assert(args!=null && args.size()==1); //the VP from which to get the verb
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof VP) return ((VP) thing).getVerb();
				else if (thing instanceof Sentence) {
					POS vp=((Sentence)thing).getVerbPhrase();
					if (vp!=null && vp instanceof VP) return ((VP)vp).getVerb();
				}
			}
			break;
		case TYPE_OF:
			assert(args!=null && args.size()==1); //the thing to get the type
			arg=args.get(0);
			if (arg!=null) { 
				Object thing=(Object)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing!=null) return thing.getClass().getName().toLowerCase();
			}
			return null;
		case LENGTH:
			assert(args!=null && args.size()==1); //the thing to calculate the length
			arg=args.get(0);
			if (arg!=null) { 
				Object thing=arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof Collection) {
					return ((Collection)thing).size();
				}
			}
			break;
		case REFERENCES:
			return state.getReferencesForState(state.coord);
		case REFERTO:
			assert(args!=null && args.size()==1); //the NP for which we return the reference in case it's a pronoun. 
			arg=args.get(0);
			if (arg!=null) { 
				Object thing=arg.generateSyntax(p,state,variables,asTrigger);
				if (thing!=null && thing instanceof NP && ((NP)thing).isPronoun()) {
					return ((NP)thing).getReference();
				}
				return thing;
			}
			return null;
		case FIRST:
			assert(args!=null && args.size()==1); //the thing to get the first
			arg=args.get(0);
			if (arg!=null) { 
				Object thing=arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof Collection && !((Collection) thing).isEmpty()) {
					return ((Collection)thing).iterator().next();
				}
			}
			return null;
		case GET:
			assert(args!=null && args.size()==2); //the thing to get the first
			arg=args.get(0);
			SyntaxBuilder index=args.get(1);
			if (arg!=null && index!=null) { 
				Object thing=arg.generateSyntax(p,state,variables,asTrigger);
				Object ia=index.generateSyntax(p,state,variables,asTrigger);
				if (ia!=null && ia instanceof String) {
					try {
						ia=Integer.parseInt((String) ia);
					} catch (Exception e) {
					}
				}
				if (thing instanceof Collection && !((Collection) thing).isEmpty() && ia!=null && ia instanceof Integer) {
					Iterator it = ((Collection)thing).iterator();
					Object ret1=null;
					Integer i=(Integer)ia;
					while(i!=null && i>=0 && it.hasNext()) {
						ret1=it.next();
						i--;
					}
					return (i<0)?ret1:null;
				}
			}
			return null;
		case SUBLIST:
			assert(args!=null && args.size()==3); //the list to sublist, the start and the end
			arg=args.get(0);
			SyntaxBuilder start=args.get(1);
			SyntaxBuilder end=args.get(2);
			if (arg!=null && start!=null && end!=null) { 
				Object thing=arg.generateSyntax(p,state,variables,asTrigger);
				Object starta=start.generateSyntax(p,state,variables,asTrigger);
				if (starta!=null && starta instanceof String) {
					try {
						starta=Integer.parseInt((String) starta);
					} catch (Exception e) {
					}
				}
				Object enda=end.generateSyntax(p,state,variables,asTrigger);
				if (enda!=null && enda instanceof String) {
					try {
						enda=Integer.parseInt((String) enda);
					} catch (Exception e) {
					}
				}
				if (thing instanceof Collection && !((Collection) thing).isEmpty() && starta!=null && starta instanceof Integer && enda!=null && enda instanceof Integer) {
					Iterator it = ((Collection)thing).iterator();
					List ret1=null;
					Integer s=(Integer)starta;
					Integer e=(Integer)enda;
					if (e<s) e=((Collection)thing).size();
					int i=0;
					while(i<e && it.hasNext()) {
						Object item=it.next();
						if (i>=s) {
							if (ret1==null) ret1=new ArrayList();
							ret1.add(item);
						}
						i++;
					}
					return ret1;
				}
			}
			return null;
		case CCS_OF:
			assert(args!=null && args.size()==1); //the CC to get the children
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof Coordination) {
					return ((Coordination)thing).conj;
				}
			}
			return null;
		case CHILDREN_OF:
			assert(args!=null && args.size()==1); //the POS to get the children
			arg=args.get(0);
			if (arg!=null) { 
				Object thing=arg.generateSyntax(p,state,variables,asTrigger);
				if (thing!=null && thing instanceof POS) {
					return ((POS)thing).getChildren();
				}
			}
			return null;
		case CONJ_OF:
			assert(args!=null && args.size()==1); //the CC to get the children
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,state,variables,asTrigger);
				if (thing instanceof Coordination) {
					return ((Coordination)thing).getFunction().toString().toLowerCase();
				}
			}
			return null;
		case MK_CC:
			assert(args!=null && args.size()>1); //the conjunction plus its arguments
			ret=new Coordination();
			SyntaxBuilder conjArg=args.get(0);
			if (conjArg!=null) { 
				Object conj=conjArg.generateSyntax(p,state,variables,asTrigger);
				((Coordination)ret).setFunction(conj.toString());
			}
			for(int i=1;i<args.size();i++) {
				SyntaxBuilder aArg=args.get(i);
				if (aArg!=null) { 
					Object a=aArg.generateSyntax(p,state,variables,asTrigger);
					if (a!=null) {
						if (a instanceof List) {
							for(Object thing:(List)a) {
								((Coordination)ret).add((POS)thing);
							}
						} else {
							((Coordination)ret).add((POS)a);
						}
					}
				}
			}
			//check that the created conjunction has more than 1 child. If it has 0 or 1 children only, return the child.
			List<POS> cs = ((Coordination)ret).getChildren();
			if (cs==null || cs.isEmpty()) return null;
			else if (cs.size()==1) return cs.get(0);
			else return ret;
		case CURRENT_POSITION:
			return (state!=null)?state.getCurrentPosition():null;
		case NEXT_POSITION:
			return (state!=null)?state.getCurrentPosition()+1:null;
		case NEXTNEXT_POSITION:
			return (state!=null)?state.getCurrentPosition()+2:null;
		case IS_IN_OBJECT_POSITION:
		case PARENT_OF:
			assert(args==null || (args!=null && args.size()==1)); //arg 1: either null (equivalent to this) or THIS or parent_of
			if (state!=null) {
				LinkedList<Integer> coord = state.coord;
				if (args!=null && !args.isEmpty()) { 
					SyntaxBuilder coorda=args.get(0);
					while(coorda!=null && coorda.operation==TYPE.PARENT_OF) {
						if (coord.isEmpty()) return null;
						coord=POS.getParentCoord(coord);
						coorda=(coorda.args!=null && !coorda.args.isEmpty())?coorda.args.get(0):null;
					}
					if (coorda!=null && coorda.operation!=TYPE.THIS) {
						System.err.println("invalid argument of parent_of function: "+args.get(0));
						return null; 
					}
				}
				if (coord!=null && !coord.isEmpty()) {
					if (operation==TYPE.IS_IN_OBJECT_POSITION) {
						return isObjectPosition(coord,state.root.getLast());
					} else {
						return state.root.getLast().getElementAt(POS.getParentCoord(coord));
					}
				}
			}
			return null;
		case ADD_MOD:
			assert(args!=null && args.size()==2); //arg 1: NP or VP to which the mod (arg 2) should be attached. 
			SyntaxBuilder thinga=args.get(0);
			Object thing=thinga.generateSyntax(p,state,variables,asTrigger);
			SyntaxBuilder moda=args.get(1);
			Object mod=moda.generateSyntax(p,state,variables,asTrigger);
			if (thing instanceof NP) {
				thing=((NP) thing).clone();
				((NP) thing).addModifier(mod,true);
			} else if (thing instanceof VP) {
				thing=((VP) thing).clone();
				((VP) thing).addModifier(mod.toString());
			}
			return thing;
		case CONTAINS:
			assert(args!=null && args.size()==2); //arg 1: the collection, arg 2: the thing (or collection) we are looking to see if it's in the collection (arg 1)
			thinga=args.get(0);
			Object collection=thinga.generateSyntax(p,state,variables,asTrigger);
			moda=args.get(1);
			thing=moda.generateSyntax(p,state,variables,asTrigger);
			if (collection!=null && collection instanceof Collection && thing!=null) {
				if (thing instanceof Collection) {
					Iterator it=((Collection) thing).iterator();
					while(it.hasNext()) {
						Object x=it.next();
						if (!isIncollection(x, (Collection) collection)) return false;
					}
					return true;
				} else {
					return isIncollection(thing,(Collection) collection);
				}
			}
			return false;
		case POSSIBLEREFS:
			assert(args!=null && args.size()==1); //arg 1: the thing to use to search for possible references (that is, the elements in the references that could be referred to by using thing) 
			thinga=args.get(0);
			thing=thinga.generateSyntax(p,state,variables,asTrigger);
			if (thing!=null && state!=null && thing instanceof NP) {
				Collection<ReferenceItem> result = getPossibleReferencesFor((NP)thing, state);
				if (result!=null) {
					List<NP> r=new ArrayList<NP>();
					for(ReferenceItem o:result) {
						r.add(o.getOriginal());
					}
					return r;
				}
			}
			return null;
		case IS_PR:
			assert(args!=null && args.size()==1); //the thing to test to se if it's a pronoun 
			thinga=args.get(0);
			thing=thinga.generateSyntax(p,state,variables,asTrigger);
			if (thing!=null && thing instanceof POS) {
				return ((POS)thing).isPronoun();
			}
			return false;
		case IS_RF:
			assert(args!=null && args.size()==1); //the thing to test to se if it's a reflexive pronoun 
			thinga=args.get(0);
			thing=thinga.generateSyntax(p,state,variables,asTrigger);
			if (thing!=null && thing instanceof POS) {
				return ((POS)thing).isReflexivePronoun();
			}
			return false;
		case ISPADJ:
			assert(args!=null && args.size()==1); //the VP/Sentence that we are checking to see if it has a verb that is an adjective (predicate adjective) 
			thinga=args.get(0);
			thing=thinga.generateSyntax(p,state,variables,asTrigger);
			if (thing!=null) {
				if (thing instanceof VP && ((VP) thing).isAdjectivePredicate()) return true;
				else if (thing instanceof Sentence && ((Sentence)thing).getVerbPhrase()!=null && ((Sentence)thing).getVerbPhrase() instanceof VP && ((VP)((Sentence)thing).getVerbPhrase()).isAdjectivePredicate()) return true;
			}
			return false;
		case NEGATED_VP:
			assert(args!=null && args.size()==1); //the VP that we are checking to see if it's negated. 
			thinga=args.get(0);
			thing=thinga.generateSyntax(p,state,variables,asTrigger);
			if (thing!=null && thing instanceof VP) return ((VP)thing).isNegated();
			else return false;
			/*
		case TOPPMATCH:
			assert(args!=null && args.size()==2); //arg 1: the collection, arg 2: the thing we are using as reference to see which item(s) in the collection match the most properties with arg 2.
			thinga=args.get(0);
			collection=thinga.generateSyntax(p,references,state,variables);
			moda=args.get(1);
			thing=moda.generateSyntax(p,references,state,variables);
			if (collection!=null && collection instanceof Collection && thing!=null && thing instanceof POS) {
				Properties ps = state.lex.getProperties((POS) thing);
				Iterator<Object> it=((Collection)collection).iterator();
				TopMatch r=new TopMatch();
				while(it.hasNext()) {
					Object c=it.next();
					if (c!=null && c instanceof POS) {
						Properties cps = state.lex.getProperties((POS)c);
						int count=countPropertiesMatch(ps,cps);
						r.add((POS) c, count);
					}
				}
				return r.getThings();
			}
			return null;
			*/
		case HAS_PROPERTY:
			assert(args!=null && args.size()==3);
			SyntaxBuilder obja=args.get(0); // arg 1: the obj....DO WE NEED THIS?
			SyntaxBuilder propa=args.get(1); // arg2: the specific property
			SyntaxBuilder valuea=args.get(2); // arg3: value of property

			Object obj=obja.generateSyntax(p,state,variables,asTrigger);
			Object prop=propa.generateSyntax(p,state,variables,asTrigger);
			Object value=valuea.generateSyntax(p,state,variables,asTrigger);
			if (obj!=null && obj instanceof POS && prop!=null && prop instanceof String && value!=null && value instanceof String) {
				Properties map = state.lex.getProperties((POS)obj);
				if (map!=null) return map.hasPropertyValue((String)prop,(String)value);
			}
			return false;
		case EVERY:
			assert(args!=null && args.size()==3);
			SyntaxBuilder lista=args.get(0); // arg 1: the list on which to loop
			SyntaxBuilder vara=args.get(1); // arg2: the name of the variable to use to assign each element in the list
			SyntaxBuilder functiona=args.get(2); // arg3: the function to execute for each child
			Object list=lista.generateSyntax(p,state,variables,asTrigger);
			Object var=vara.generateSyntax(p,state,variables,asTrigger);
			if (list!=null && list instanceof Collection && var!=null && var instanceof String) {
				if (variables!=null && variables.containsKey(var)) System.err.println("duplicated loop variable: "+this);
				for(Object x:(Collection)list) {
					if (variables==null) variables=new HashMap<String, Object>();
					variables.put((String) var,x);
					Object r=functiona.generateSyntax(p,state,variables,asTrigger);
					variables.remove(var);
					if (r==null || !(r instanceof Boolean) || !((Boolean)r)) return false;
				}
				return true;
			}
			return false;
		case MAP:
			assert(args!=null && args.size()==3);
			lista=args.get(0); // arg 1: the list on which to loop
			vara=args.get(1); // arg2: the name of the variable to use to assign each element in the list
			functiona=args.get(2); // arg3: the function to execute for each child
			list=lista.generateSyntax(p,state,variables,asTrigger);
			var=vara.generateSyntax(p,state,variables,asTrigger);
			if (list!=null && list instanceof Collection && var!=null && var instanceof String) {
				if (variables!=null && variables.containsKey(var)) System.err.println("duplicated loop variable: "+this);
				List<Object> things=new ArrayList<Object>();
				for(Object x:(Collection)list) {
					if (variables==null) variables=new HashMap<String, Object>();
					variables.put((String) var,x);
					Object r=functiona.generateSyntax(p,state,variables,asTrigger);
					variables.remove(var);
					things.add(r);
				}
				return things;
			}
			return null;
		case EQ:
			assert(args!=null && args.size()==2);
			SyntaxBuilder arg0a=args.get(0); 
			SyntaxBuilder arg1a=args.get(1);
			Object arg0=arg0a.generateSyntax(p,state,variables,asTrigger);
			Object arg1=arg1a.generateSyntax(p,state,variables,asTrigger);
			if (arg0==null || arg1==null) return (arg0==arg1)?true:null;
			else {
				try {
					Double x1=Double.parseDouble(arg0.toString());
					Double x2=Double.parseDouble(arg1.toString());
					return x1.compareTo(x2)==0;
				} catch (Exception e) {}
				return  myCollator.compare(arg0.toString(),arg1.toString())==0;
			}
		case GT:
			assert(args!=null && args.size()==2);
			arg0a=args.get(0);
			arg1a=args.get(1);
			arg0=arg0a.generateSyntax(p,state,variables,asTrigger);
			arg1=arg1a.generateSyntax(p,state,variables,asTrigger);
			if (arg0==null || arg1==null) return null;
			else {
				try {
					Double x1=Double.parseDouble(arg0.toString());
					Double x2=Double.parseDouble(arg1.toString());
					return x1>x2;
				} catch (Exception e) {}
				return myCollator.compare(arg0.toString(),arg1.toString())>0;
			}
		case GE:
			assert(args!=null && args.size()==2);
			arg0a=args.get(0);
			arg1a=args.get(1);
			arg0=arg0a.generateSyntax(p,state,variables,asTrigger);
			arg1=arg1a.generateSyntax(p,state,variables,asTrigger);
			if (arg0==null || arg1==null) return (arg0==arg1)?true:null;
			else {
				try {
					Double x1=Double.parseDouble(arg0.toString());
					Double x2=Double.parseDouble(arg1.toString());
					return x1>=x2;
				} catch (Exception e) {}
				return myCollator.compare(arg0.toString(),arg1.toString())>=0;
			}
		case LT:
			assert(args!=null && args.size()==2);
			arg0a=args.get(0);
			arg1a=args.get(1);
			arg0=arg0a.generateSyntax(p,state,variables,asTrigger);
			arg1=arg1a.generateSyntax(p,state,variables,asTrigger);
			if (arg0==null || arg1==null) return null;
			else {
				try {
					Double x1=Double.parseDouble(arg0.toString());
					Double x2=Double.parseDouble(arg1.toString());
					return x1<x2;
				} catch (Exception e) {}
				return myCollator.compare(arg0.toString(),arg1.toString())<0;
			}
		case LE:
			assert(args!=null && args.size()==2);
			arg0a=args.get(0);
			arg1a=args.get(1);
			arg0=arg0a.generateSyntax(p,state,variables,asTrigger);
			arg1=arg1a.generateSyntax(p,state,variables,asTrigger);
			if (arg0==null || arg1==null) return (arg0==arg1)?true:null;
			else {
				try {
					Double x1=Double.parseDouble(arg0.toString());
					Double x2=Double.parseDouble(arg1.toString());
					return x1<=x2;
				} catch (Exception e) {}
				return myCollator.compare(arg0.toString(),arg1.toString())<=0;
			}
		case AND:
			for(SyntaxBuilder a:args) {
				Object r=a.generateSyntax(p,state,variables,asTrigger);
				if (r==null || !(r instanceof Boolean) || !((Boolean)r)) return false;
			}
			return true;
		case OR:
			for(SyntaxBuilder a:args) {
				Object r=a.generateSyntax(p,state,variables,asTrigger);
				if (r!=null && (r instanceof Boolean) && ((Boolean)r)) return true;
			}
			return false;
		case NOT:
			assert(args!=null && args.size()==1);
			arg0a=args.get(0);
			Object r=arg0a.generateSyntax(p,state,variables,asTrigger);
			if (r!=null && r instanceof Boolean) return !((Boolean)r);
			return null;
		case PRINT:
			assert(args!=null && args.size()==1); //the thing to print
			arg=args.get(0);
			String sret=((arg!=null)?arg.toString():null)+":=";
			thing=null;
			if (arg!=null) { 
				thing=arg.generateSyntax(p,state,variables,asTrigger);
				sret+=(thing!=null)?thing.toString():null;
			}
			System.out.println(sret);
			return thing;
		default:
			System.err.println(operation+" not implemented.");
			break;
		}
		return ret;
	}
	
	private void addComplementTo(POS complement, POS recipient) {
		if (recipient instanceof Sentence) {
			((Sentence)recipient).addComplement(complement);
		} else if (recipient instanceof VP) {
			((VP)recipient).addArgument(complement);
		}
	}

	private Collection<ReferenceItem> getPossibleReferencesFor(NP thing,TraversalState state) {
		Collection<ReferenceItem> result = state.getReferencesForState(state.coord).getPossibleReferencesFor(thing, state.lex);
		if (result!=null) {
			Set<String> ups=null;
			for(ReferenceItem ri:result) {
				Set<String> riUps=ri.getUnavailableProperties();
				if (riUps!=null) {
					if (ups==null) ups=new HashSet<String>();
					ups.addAll(riUps);
				}
			}
			if (ups!=null && !ups.isEmpty()) {
				ReferenceItem it=new ReferenceItem(thing, state.lex,null);
				boolean isPronoun=thing.isPronoun();
				for(String up:ups) {
					// set properties as transparent except for gender of pronouns.
					if (!isPronoun || !(up.equals(Properties.pnameFEMALEPROPERTY) || up.equals(Properties.pnameMALEPROPERTY))) {
						it.updateProperty(up, Properties.UNKNOWN);
					}
				}
				result = state.getReferencesForState(state.coord).getPossibleReferencesFor(it);
			}
		}
		return result;
	}

	private POS getPronoun(POS t, TraversalState state) {
		POS ret=null;
		if (t!=null && t instanceof POS) {
			boolean objectPosition=isObjectPosition(state.coord,state.root.getLast());
			if (t instanceof NP) {
				boolean possessive=((NP) t).isPossessiveForm();
				if (((NP)t).isPlural(state.lex)) ret=new NP(objectPosition?"them":"they",t);
				Boolean f=((NP)t).isFemale(state.lex);
				if (f==null) ret=new NP("it",t);
				else if (f) ret=new NP(objectPosition?"her":"she",t);
				else ret=new NP(objectPosition?"him":"he",t);
				((NP)ret).setPossessiveForm(possessive);
			} else if (t instanceof Coordination) {
				Coordination tc=(Coordination)t;
				List<POS> cs = tc.getChildren();
				if (cs!=null) {
					if (cs.size()>1) ret=new NP(objectPosition?"them":"they",t);
					else if (cs.size()==1) ret=getPronoun(cs.get(0),state);
				}
			}
		}
		return ret;
	}

	public static boolean isObjectPosition(List<Integer> c,POS root) {
		LinkedList<Integer> parentCoord=null;
		while((parentCoord = POS.getParentCoord(c))!=null) {
			POS thing = root.getElementAt(parentCoord);
			if (thing instanceof VP) return true;
			else if (thing instanceof Sentence) break;
			c=parentCoord;
		}
		return false;
	}

	/**
	 * traverses the parents
	 *  if in object position and
	 *   for each sentence, if the sentence has same subject then return true
	 *    note that a vp that is the argument of a sentence (in position 2) and with an NP in position 1 (object) will inherit the object as its subject. 
	 *     Unless the verb of the sentence has property objectcontrol=false. in that case it inherits the subject of the sentence as its own subject.
	 * return false 
	 * @param referencedThing
	 * @param state
	 * @return
	 */
	private static boolean isReflexive(POS referencedThing,TraversalState state) {
		boolean isObject=isObjectPosition(state.coord,state.root.getLast());
		if (isObject) {
			POS subject = getSubjectOfThisObject(state);
			return (subject!=null && subject.equals(referencedThing));
		}
		return false;
	}
	
	public static POS getSubjectOfThisObject(TraversalState state) {
		LinkedList<Integer> c=state.coord;
		POS root=state.root.getLast();
		while(true) {
			POS thing = POS.getParent(c,root);
			if (thing!=null) {
				if (thing instanceof VP) return getSubjectOfVP(thing,POS.getParentCoord(state.coord),state);
				else if (thing instanceof NP) {
					c=POS.getParentCoord(c);
					continue;
				}
			}
			break;
		}
		System.err.println("parent of object is not a VP. Failed to find a subject");
		return null;
	}

	private static POS getSubjectOfVP(POS thing, LinkedList<Integer> thingCoord, TraversalState state) {
		POS root=state.root.getLast();
		POS thingParent=POS.getParent(thingCoord, root);
		while(thingParent!=null) {
			if (thingParent instanceof Sentence) {
				POS subject=((Sentence)thingParent).getSubject();
				if (subject!=null && subject instanceof NP) {
					if (subject.isPronoun()) {
						return ((NP)subject).getReference();
					} else {
						return subject;
					}
				}
				return null;
			} else if (thingParent instanceof VP) {
				if (((VP)thingParent).getIndirectObject()==thing && (((VP)thingParent).getObject()!=null && ((VP)thingParent).getObject() instanceof NP)) { 
					Properties ps = state.lex.getProperties(thingParent);
					if (ps.hasPropertyValue(Properties.pnameOBJECTCONTROL, Properties.TRUE) ||
							ps.hasPropertyValue(Properties.pnameOBJECTCONTROL, Properties.UNKNOWN)) {
						NP subject = (NP) ((VP)thingParent).getObject();
						if (subject!=null) {
							if (subject.isPronoun()) {
								return ((NP)subject).getReference();
							} else {
								return subject;
							}
						} else {
							System.err.println("failed to find a subject. Subject null.");
							return null;
						}
					}
				}
				thing=thingParent;
				thingParent=POS.getParent(thingCoord, root);
				thingCoord=POS.getParentCoord(thingCoord);
			} else if (thingParent instanceof Coordination || thingParent instanceof PP) {
				thing=thingParent;
				thingParent=POS.getParent(thingCoord, root);
				thingCoord=POS.getParentCoord(thingCoord);
			} else {
				System.err.println("failed to find a subject. Failed to find a VP chain case 1.");
				return null;
			}
		}
		System.err.println("failed to find a subject. Failed to find a VP chain case 2.");
		return null;
	}

	private int countPropertiesMatch(Properties reference,Properties other) {
		if (reference!=null && other!=null) {
			Properties intersection = new Properties(reference);
			intersection.retainAll(other);
			if (intersection!=null && !intersection.isEmpty()) {
				int count=0;
				for(Property p:intersection) {
					String rv=reference.get(p.getName());
					String ov=other.get(p.getName());
					if (rv==ov || (rv!=null && rv.equalsIgnoreCase(ov))) count++;
				}
				return count;
			}
		}
		return 0;
	}
	private class TopMatch {
		List<POS> things=null;
		int rank=-1;
		public void add(POS thing,int match) {
			if (match>rank) {
				rank=match;
				if (things==null) things=new ArrayList<POS>();
				else things.clear();
				things.add(thing);
			} else if (match==rank) {
				if (things==null) things=new ArrayList<POS>();
				things.add(thing);
			}
		}
		public List<POS> getThings() {
			return things;
		}
		public int getRank() {
			return rank;
		}
	}
	
	private boolean isIncollection(Object thing, Collection collection) {
		Iterator it=collection.iterator();
		while(it.hasNext()) {
			Object x=it.next();
			boolean t1=x==thing;
			boolean t2=x.equals(thing);
			if (t1 || t2) return true;
		}
		return false;
	}

	@Override
	public String toString() {
		String prefix="";
		if (getRow()>=0 && getColumn()>=0) prefix="(r="+getRow()+",c="+getColumn()+") ";
		return prefix+asString();
	}
	private String asString() {
		if (operation==TYPE.CNST) return cnst;
		else {
			String basic=operation+"("+args+")";
			if (changeParent()) return "change_parent("+basic+")";
			else return basic;
		}
	}
	
	public void disablesBuildersAtRows(String[] ds) {
		if (ds!=null) {
			for(String d:ds) {
				try {
					int skipped=Integer.parseInt(d);
					if (disabledBuilders==null) disabledBuilders=new HashSet<Integer>();
					disabledBuilders.add(skipped);
					if (getRow()==skipped) System.err.println("Rule at row: "+getRow()+" is disabling itself.");
				} catch (Exception e) {}
			}
		}
	}
	public Set<Integer> getDisabledBuilders() {
		return disabledBuilders;
	}

	public static void main(String[] args) throws Exception {
		SyntaxBuilder sb = new SyntaxBuilder("type", null);
		POS noun=new NP("test");
		Object z = sb.generateSyntax(noun,null,null,false);
		System.out.println(z);
		sb = new SyntaxBuilder("lf.pos.np", null);
		System.out.println(sb.generateSyntax(null,null,null,false));
	}
}
