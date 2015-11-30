package lf.pos;

import java.util.List;

import nlg2.NLG2Lexicon;
import nlg3.properties.Properties;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;

public interface POSI {
	public NLGElement toSimpleNLG(NLGFactory nlgFactory);
	public boolean equals(Object obj);
	public POS clone();
	public List<POS> getChildren();
	public void put(List<Integer> coord, POS newd) throws Exception;
	public void updateChild(int pos,POS child) throws Exception;
	public Properties getProperties(NLG2Lexicon lex);
	
	public enum CONJ {
		AND,OR,LIST,BECAUSE,INORDER,ANDSO,AS,SINCE;

		@Override
		public String toString() {
			if (this==INORDER) return "IN ORDER";
			else if (this==ANDSO) return "AND SO";
			return super.toString();
		};
	}
	
	public enum DT {
		THE,A,NULL;
	}

	public boolean isPronoun();
	boolean isReflexivePronoun();
}
