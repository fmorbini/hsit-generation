package nlg1;

import java.util.ArrayList;
import java.util.List;

import lf.pos.Coordination;
import lf.pos.POS;
import simplenlg.framework.DocumentElement;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.realiser.english.Realiser;

/**
 * uses simple nlg as a realizer.
 * @author morbini
 *
 */
public class Realizer {
	private Realiser realiser;
	private NLGFactory nlgFactory;
	private Lexicon lexicon;

	public Realizer() {
		lexicon = Lexicon.getDefaultLexicon();
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
	}
	
	public Realiser getRealiser() {
		return realiser;
	}
	
	public String process(List<POS> sentences) {
		List<String> ss=realizeIndependent(sentences);
		//List<DocumentElement> ss=realize(sentences);
		//DocumentElement output=nlgFactory.createParagraph(ss);
		//return realiser.realise(output).getRealisation();
		StringBuffer ret=new StringBuffer();
		for(String s:ss) {
			ret.append(((ret.length()==0)?"":" ")+s);
		}
		return ret.toString();
	}

	private List<String> realizeIndependent(List<POS> sentences) {
		List<String> ret=null;
		for(POS s:sentences) {
			if (s!=null && s instanceof Coordination && ((Coordination)s).isList()) {
				List<String> tmp = realizeIndependent(s.getChildren());
				if (tmp!=null) {
					if (ret==null) ret=new ArrayList<String>();
					ret.addAll(tmp);
				}
			} else {
				NLGElement e = s.toSimpleNLG(nlgFactory);
				if (ret==null) ret=new ArrayList<String>();
				DocumentElement d = nlgFactory.createSentence(e);
				String r=realiser.realise(d).getRealisation();
				ret.add(r);
			}
		}
		return ret;
	}

	private List<DocumentElement> realize(List<POS> sentences) {
		List<DocumentElement> ret=null;
		for(POS s:sentences) {
			if (s!=null && s instanceof Coordination && ((Coordination)s).isList()) {
				List<DocumentElement> tmp = realize(s.getChildren());
				if (tmp!=null) {
					if (ret==null) ret=new ArrayList<DocumentElement>();
					ret.addAll(tmp);
				}
			} else {
				NLGElement e = s.toSimpleNLG(nlgFactory);
				if (ret==null) ret=new ArrayList<DocumentElement>();
				
				DocumentElement d = nlgFactory.createSentence(e);
				String r=realiser.realise(d).getRealisation();
				System.out.println(r);

				ret.add(nlgFactory.createSentence(e));
			}
		}
		return ret;
	}
	
	public static void main(String[] args) {
		Lexicon lexicon = Lexicon.getDefaultLexicon();
		NLGFactory nlgFactory = new NLGFactory(lexicon);
		Realiser realiser = new Realiser(lexicon);
		NPPhraseSpec np = nlgFactory.createNounPhrase("man");
		np.setDeterminer("the");
		np.addPreModifier("bald");
		NPPhraseSpec np2 = nlgFactory.createNounPhrase(np);
		np2.addPreModifier("tall");
		realiser.setCommaSepPremodifiers(false);
		realiser.setDebugMode(true);
		System.out.println(realiser.realise(np2).getRealisation());
	}
}
