package lf.pos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nlg2.NLG2Lexicon;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.phrasespec.PPPhraseSpec;

public class PP extends POS {

	private POS complement;
	private String preposition; 

	@Override
	public NLGElement toSimpleNLG(NLGFactory nlgFactory) {
        PPPhraseSpec pp = nlgFactory.createPrepositionPhrase();
        pp.addComplement(complement.toSimpleNLG(nlgFactory));
        pp.setPreposition(preposition);
		return pp;
	}

	@Override
	public boolean equals(Object o) {
		if (o!=null && o instanceof PP) {
			return ((preposition==((PP)o).preposition || preposition.equals(((PP)o).preposition)) &&
					(complement==((PP)o).complement || complement.equals(((PP)o).complement)));
		}
		return false;
	}

	@Override
	public POS clone() {
		PP ret=new PP();
		ret.complement=complement.clone();
		ret.preposition=preposition;
		return ret;
	}
	
	public void setComplement(POS complement) {
		this.complement = complement;
	}
	public void setPreposition(String preposition) {
		this.preposition = preposition;
	}
	public String getPreposition() {
		return preposition;
	}
	public POS getComplement() {
		return complement;
	}

	@Override
	public List<POS> getChildren() {
		List<POS> ret=null;
		if (complement!=null) {
			ret=new ArrayList<POS>();
			ret.add(complement);
		}
		return ret;
	}
	@Override
	public void updateChild(int pos, POS child) {
		if (complement!=null && pos==0) {
			complement=child;
		} else System.err.println("error updating child of pp at position: "+pos);
	}
	
	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		ret.append("PP(");
		if (preposition!=null) {
			ret.append(preposition.toString());
		}
		if (complement!=null) {
			ret.append(" "+complement.toString());
		}
		ret.append(")");
		return ret.toString();
	}

}
