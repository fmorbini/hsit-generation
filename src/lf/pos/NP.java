package lf.pos;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import edu.usc.ict.nl.util.StringUtils;
import nlg2.NLG2Lexicon;
import nlg3.properties.Properties;
import nlg3.properties.Property;
import simplenlg.features.Feature;
import simplenlg.features.InternalFeature;
import simplenlg.features.LexicalFeature;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.phrasespec.NPPhraseSpec;

public class NP extends POS {
	private String noun;
	private DT determiner;
	private List<POS> mods=null;
	private POS reference=null; 
	private boolean reflexive=false;
	private boolean possessiveForm=false;
	
	public NP(String noun) {
		this.setNoun(noun);
	}
	public NP(String pronoun,POS reference) {
		this(pronoun);
		this.reference=reference;
	}

	@Override
	public POS clone() {
		NP ret=new NP(getNoun());
		ret.reference=this.reference;
		ret.setDeterminer(this.getDeterminer());
		ret.reflexive=this.reflexive;
		ret.possessiveForm=this.possessiveForm;
		if (getMods()!=null) {
			ret.setMods(new ArrayList<POS>());
			for(POS m:getMods()) {
				ret.getMods().add(m.clone());
			}
		}
		return ret;
	}
	

	public void setReflexive(boolean b) {
		reflexive=b;
	}
	public boolean isReflexive() {
		return reflexive;
	}
	public void setPossessiveForm(boolean possessiveForm) {
		this.possessiveForm = possessiveForm;
	}
	public boolean isPossessiveForm() {
		return possessiveForm;
	}

	public NLGElement toSimpleNLG(NLGFactory nlgFactory) {
		if (isPronoun()) {
			NLGElement x = nlgFactory.createWord(getNoun(),LexicalCategory.PRONOUN);
			x.setFeature(Feature.POSSESSIVE, isPossessiveForm());
			x.setFeature(LexicalFeature.REFLEXIVE, isReflexive());
			NPPhraseSpec ret = nlgFactory.createNounPhrase(getNoun());
			ret.setFeature(Feature.PRONOMINAL, true);
			ret.setFeature(Feature.POSSESSIVE, isPossessiveForm());
			ret.setFeature(LexicalFeature.REFLEXIVE, isReflexive());
			return x;
		} else {
			// for proper names don't say the determiner.
			if ((getMods()==null || getMods().isEmpty()) && !StringUtils.isEmptyString(getNoun()) && !getNoun().contains(" ") && !StringUtils.isAllUpperCase(getNoun()) && Character.isUpperCase(getNoun().charAt(0))) {
				setDeterminer(DT.NULL);
			}
			NPPhraseSpec thisPhrase=nlgFactory.createNounPhrase(getNoun());
			if (getDeterminer()!=null) {
				if (getDeterminer()!=DT.NULL) {
					thisPhrase.setDeterminer(getDeterminer().toString().toLowerCase());
				}
			} else thisPhrase.setDeterminer(DT.THE.toString().toLowerCase());
			if (getMods()!=null) {
				for(POS m:getMods()) {
					if (m instanceof ADJ) {
						thisPhrase.addPreModifier(m.toString());
					} else {
						NLGElement base = m.toSimpleNLG(nlgFactory);
						boolean addOwn=false;
						if (m.isPronoun() && ((NP)m).isPossessiveForm()) {
							if (((NP)m).isReflexive()) {
								base.setFeature(Feature.POSSESSIVE, false);
								base.setFeature(LexicalFeature.REFLEXIVE, false);
								addOwn=true;
							}
						}
						thisPhrase.addPreModifier(base);
						if (addOwn) thisPhrase.addPreModifier("own");
					}
				}
			}
			thisPhrase.setFeature(Feature.POSSESSIVE, isPossessiveForm());
			return thisPhrase;
		}
	}
	
	public void addModifier(String m) {
		addModifier(m, false);
	}
	public void addModifier(Object m,boolean addBefore) {
		if (m!=null) {
			POS mo=null;
			if (m instanceof String) mo=new ADJ((String) m);
			else if (m instanceof POS) mo=(POS) m;
			if (getMods()==null) setMods(new ArrayList<POS>());
			if (addBefore) getMods().add(0,mo);
			else getMods().add(mo);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o!=null && o instanceof NP) {
			NP thisR=(NP) (isPronoun()?getReference():this);
			NP otherR=(NP) (((NP)o).isPronoun()?((NP)o).getReference():o);
			return thisR.internalEqual(otherR);
		}
		return false;
	}

	private boolean internalEqual(NP o) {
		if (o!=null) {
			boolean r=((getNoun()==o.getNoun() || getNoun().equals(o.getNoun())) &&
					(getDeterminer()==o.getDeterminer() || getDeterminer().equals(o.getDeterminer())) &&
					((getMods()==o.getMods()) || ((getMods()!=null && o.getMods()!=null) && getMods().size()==o.getMods().size())));
			if (r) {
				if (getMods()!=null) {
					for(int i=0;i<getMods().size();i++) {
						if (!getMods().get(i).equals(o.getMods().get(i))) return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public static final NP EMPTYNP=createEmptyNP(false);
	public static final NP EMPTYNPSUBJECT=createEmptyNP(true);
	private static NP createEmptyNP(boolean asSubject) {
		NP ret=new NP(asSubject?"someone":"something");
		((NP)ret).setDeterminer(DT.NULL);
		return ret;
	}
	
	public POS getReference() {
		return reference;
	}
	
	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		ret.append("NP(");
		if (isPronoun() && isReflexive()) {
			ret.append("_REFLEXIVE_");
		}
		if (isPossessiveForm()) {
			ret.append("_POSSESSIVE_");
		}
		if (getDeterminer()!=null) {
			if (getDeterminer()!=DT.NULL) {
				ret.append(getDeterminer().toString().toLowerCase());
			}
		} else ret.append(DT.THE.toString().toLowerCase());
		ret.append(" "+getNoun());
		if (getMods()!=null) for(POS m:getMods()) {
			ret.append("MOD:("+m+")");
		}
		ret.append(")");
		return ret.toString();
	}
	
	@Override
	public List<POS> getChildren() {
		return getMods();
	}

	private boolean isTriangle() {
		return getNoun().matches("^triangle(')?$");
	}

	private boolean isCircle() {
		return getNoun().matches("^circle(')?$");
	}

	@Override
	public Properties getProperties(NLG2Lexicon lex) {
		Properties ret = lex.getProperties(getNoun());
		if (isCircle()) {
			ret=ret.clone();
			ret.add(new Property(Properties.pnameFEMALEPROPERTY, Properties.TRUE));
			ret.add(new Property(Properties.pnameMALEPROPERTY, Properties.FALSE));
		} else if (isTriangle()) {
			ret=ret.clone();
			ret.add(new Property(Properties.pnameMALEPROPERTY, Properties.TRUE));
			ret.add(new Property(Properties.pnameFEMALEPROPERTY, Properties.FALSE));
		} else if (isPronoun()) {
			if (femalePronouns.matcher(getNoun()).matches()) {
				ret=ret.clone();
				ret.add(new Property(Properties.pnameFEMALEPROPERTY, Properties.TRUE));
				ret.add(new Property(Properties.pnameMALEPROPERTY, Properties.FALSE));
				ret.add(new Property(Properties.pnameAGENTPROPERTY, Properties.TRUE));
			} else if (malePronouns.matcher(getNoun()).matches()) {
				ret=ret.clone();
				ret.add(new Property(Properties.pnameMALEPROPERTY, Properties.TRUE));
				ret.add(new Property(Properties.pnameFEMALEPROPERTY, Properties.FALSE));
				ret.add(new Property(Properties.pnameAGENTPROPERTY, Properties.TRUE));
			}
		}
		return ret;
	}

	public Boolean isFemale(NLG2Lexicon lex) {
		Properties ps = getProperties(lex);
		boolean m=ps.hasPropertyValue(Properties.pnameMALEPROPERTY, Properties.TRUE);
		boolean f=ps.hasPropertyValue(Properties.pnameFEMALEPROPERTY, Properties.TRUE);
		if (!m && f) return true;
		else if (m && !f) return false;
		else return null;
	}
	public Boolean isMale(NLG2Lexicon lex) {
		Boolean r=isFemale(lex);
		return (r!=null)?!r:null;
	}
	
    private static final Pattern femalePronouns = Pattern.compile("^(she)|(her)|(hers)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern femaleReflexivePronouns = Pattern.compile("^(herself)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern malePronouns = Pattern.compile("^(he)|(him)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern maleReflexivePronouns = Pattern.compile("^(himself)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern neutralPronouns = Pattern.compile("^(i)|(me)|(mine)|(my)|(you)|(your)|(yours)|(it)|(its)|(we)|(our)|(ours)|(they)|(them)|(their)|(theirs)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern neutralReflexivePronouns = Pattern.compile("^(myself)|(yourself)|(yourselves)|(itself)|(ourselves)|(themselves)$", Pattern.CASE_INSENSITIVE);
	@Override
	public boolean isPronoun() {
		return (getDeterminer()==null || getDeterminer()==DT.NULL) && (getMods()==null || getMods().isEmpty()) &&
				(neutralPronouns.matcher(getNoun()).matches() || femalePronouns.matcher(getNoun()).matches() || malePronouns.matcher(getNoun()).matches() ||
						neutralReflexivePronouns.matcher(getNoun()).matches() || femaleReflexivePronouns.matcher(getNoun()).matches() || maleReflexivePronouns.matcher(getNoun()).matches());
	}
	@Override
	public boolean isReflexivePronoun() {
		return isReflexive() && isPronoun();
	}

	public boolean isPlural(NLG2Lexicon lex) {
		return false;
	}
	public String getNoun() {
		return noun;
	}
	public void setNoun(String noun) {
		this.noun = noun;
	}
	public DT getDeterminer() {
		return determiner;
	}
	public void setDeterminer(DT determiner) {
		this.determiner = determiner;
	}
	public List<POS> getMods() {
		return mods;
	}
	public void setMods(List<POS> mods) {
		this.mods = mods;
	}
}
