package lf.pos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import edu.usc.ict.nl.util.StringUtils;
import nlg2.NLG2Lexicon;
import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.phrasespec.SPhraseSpec;

public class Sentence extends POS {

	private POS subject=null;
	private POS verbPhrase=null;
	public List<POS> complements=null;

	@Override
	public POS clone() {
		Sentence ret=new Sentence();
		ret.setSubject((getSubject()!=null)?getSubject().clone():null);
		try {
			ret.setVerbPhrase((getVerbPhrase()!=null)?getVerbPhrase().clone():null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (complements!=null) {
			ret.complements=new ArrayList<POS>();
			for(POS c:complements) ret.complements.add(c.clone());
		}
		return ret;
	}

	public void addComplement(POS m) {
		if (m!=null) {
			if (complements==null) complements=new ArrayList<POS>();
			complements.add(m);
		}
	}
	
	public SPhraseSpec toSimpleNLG(NLGFactory nlgFactory) {
		SPhraseSpec s=nlgFactory.createClause();
		s.setSubject(getSubject().toSimpleNLG(nlgFactory));
		NLGElement vpnlg = getVerbPhrase().toSimpleNLG(nlgFactory);
		if (complements!=null) {
			if (vpnlg instanceof CoordinatedPhraseElement) {
				for(POS c:complements) ((CoordinatedPhraseElement)vpnlg).addComplement(c.toSimpleNLG(nlgFactory));
				s.setVerbPhrase(vpnlg);
			} else {
				s.setVerbPhrase(vpnlg);
				for(POS c:complements) s.addComplement(c.toSimpleNLG(nlgFactory));
			}
		} else {
			s.setVerbPhrase(vpnlg);
		}
		return s;
	}
	public void addSubject(POS s) {
		if (getSubject()==null) setSubject(s);
		else if (getSubject() instanceof Coordination) {
			((Coordination)getSubject()).add(s);
		} else {
			NP olds=(NP) getSubject();
			setSubject(new Coordination());
			((Coordination)getSubject()).add(olds);
			((Coordination)getSubject()).add(s);
		}
	}


	@Override
	public boolean equals(Object o) {
		if (o!=null && o instanceof Sentence) {
			boolean r=(getSubject()==((Sentence)o).getSubject() || getSubject().equals(((Sentence)o).getSubject())) &&
					(getVerbPhrase()==((Sentence)o).getVerbPhrase() || getVerbPhrase().equals(((Sentence)o).getVerbPhrase())) &&
					((complements==((Sentence)o).complements) || (complements!=null && ((Sentence)o).complements!=null && ((Sentence)o).complements.size()==complements.size()));
			if (r) {
				if (complements!=null) {
					for(int i=0;i<complements.size();i++) {
						if (!complements.get(i).equals(((Sentence)o).complements.get(i))) return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public void addVerbPhrase(VP vp) throws Exception {
		if (getVerbPhrase()==null) setVerbPhrase(vp);
		else {
			if (getVerbPhrase() instanceof Coordination) {
				((Coordination)getVerbPhrase()).add(vp);
			} else {
				POS oldvp=getVerbPhrase();
				setVerbPhrase(new Coordination());
				((Coordination)getVerbPhrase()).add(oldvp);
				((Coordination)getVerbPhrase()).add(vp);
			}
		}
	}

	public void addVerbModifier(String m) {
		if (getVerbPhrase()!=null) {
			Stack<POS> s=new Stack<POS>();
			s.push(getVerbPhrase());
			while (!s.isEmpty()) {
				POS x=s.pop();
				if (x instanceof VP) {
					((VP) x).addModifier(m);
				} else {
					s.addAll(((Coordination)x).conj);
				}
			}
		}
	}

	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		ret.append("CLAUSE(");
		ret.append("Subj=("+getSubject()+")");
		ret.append("Vp=("+getVerbPhrase()+")");
		ret.append("Comp=("+complements+")");
		ret.append(")");
		return ret.toString();
	}

	@Override
	public List<POS> getChildren() {
		List<POS> ret=null;
		if (getSubject()!=null) {
			if (ret==null) ret=new ArrayList<POS>();
			ret.add(getSubject());
		}
		if (getVerbPhrase()!=null) {
			if (ret==null) ret=new ArrayList<POS>();
			ret.add(getVerbPhrase());
		}
		if (complements!=null && !complements.isEmpty()) {
			if (ret==null) ret=new ArrayList<POS>();
			ret.addAll(complements);
		}
		return ret;
	}
	public POS getVerbPhrase() {
		return verbPhrase;
	}

	public void setVerbPhrase(POS verbPhrase) throws Exception {
		if (verbPhrase==null) {
			throw new Exception("attempting to set the verb phrase of a sentence to null.");
		}
		this.verbPhrase = verbPhrase;
	}

	@Override
	public void updateChild(int pos, POS child) throws Exception {
		int delta=0;
		if (getSubject()!=null) delta++;
		if (getVerbPhrase()!=null) delta++;
		if (pos<delta) {
			if (pos==0) {
				if (getSubject()!=null) setSubject(child);
				else setVerbPhrase(child);
			} else setVerbPhrase(child);
		} else {
			pos=pos-delta;
			if (complements!=null && complements.size()>pos) {
				complements.set(pos, child);
			} else System.err.println("error setting child of sentence at position: "+pos+" with delta of: "+delta);
		}
	}

	public POS getSubject() {
		return subject;
	}

	public void setSubject(POS subject) {
		if (subject==null) subject=NP.EMPTYNP;
		this.subject = subject;
	}

}
