package lf.pos;

import java.util.List;
import java.util.Map;

import nlg2.NLG2Lexicon;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.phrasespec.VPPhraseSpec;

public class ADJ extends POS {

	private String mod;
	
	public ADJ(String m) {
		this.mod=m;
	}

	@Override
	public NLGElement toSimpleNLG(NLGFactory nlgFactory) {
		VPPhraseSpec vp = nlgFactory.createVerbPhrase("be "+mod);
		return vp;
	}

	@Override
	public boolean equals(Object o) {
		if (o!=null && o instanceof ADJ) {
			return (mod==((ADJ)o).mod || mod.equals(((ADJ)o).mod));
		}
		return false;
	}

	@Override
	public POS clone() {
		return new ADJ(mod);
	}
	
	@Override
	public List<POS> getChildren() {
		return null;
	}
	
	@Override
	public String toString() {
		return mod;
	}
	
	public void set(String m) {
		this.mod=m;
	}

}
