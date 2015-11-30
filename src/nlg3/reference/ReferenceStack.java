package nlg3.reference;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import lf.pos.NP;
import lf.pos.POS;
import nlg3.NLG3Lexicon;
import nlg3.TraversalState;
import nlg3.properties.Properties;
import nlg3.properties.Property;

public class ReferenceStack extends ArrayDeque<ReferenceItem> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	

	public Collection<ReferenceItem> getPossibleReferencesFor(Object thing, NLG3Lexicon lex) {
		if (thing!=null && thing instanceof NP) {
			ReferenceItem it=new ReferenceItem((NP) thing, lex,null);
			return getPossibleReferencesFor(it);
		}
		return null;
	}
	public Collection<ReferenceItem> getPossibleReferencesFor(ReferenceItem it) {
		Set<ReferenceItem> intersection=null;
		Set<String> ps=Properties.getAllProperties();
		for(String pname:ps) {
			Property p=it.getPropertyNamed(pname);
			Set<ReferenceItem> tmp=getElementsWithThisProperty(p);
			if (tmp==null || tmp.isEmpty()) return null;
			else {
				if (intersection==null) intersection=tmp;
				else {
					intersection.retainAll(tmp);
					if (intersection.isEmpty()) break;
				}
			}
		}
		return intersection;
	}

	private Set<ReferenceItem> getElementsWithThisProperty(Property p) {
		Set<ReferenceItem> ret=null;
		String v=p.getValue();
		boolean isUnknown=v.equalsIgnoreCase(Properties.UNKNOWN);
		for(ReferenceItem e:this) {
			boolean otherIsUnknown=!e.isAvailable(p.getName());
			boolean match=isUnknown || otherIsUnknown;
			if (!match) {
				String ev=e.getPropertyValue(p.getName());
				match=(v.equalsIgnoreCase(ev));
			}
			if (match) {
				if (ret==null) ret=new HashSet<ReferenceItem>();
				ret.add(e);
			}
		}
		return ret;
	}
	
	@Override
	public ReferenceStack clone() {
		ReferenceStack ret=new ReferenceStack();
		for(ReferenceItem r:this) {
			ret.add(r.clone());
		}
		return ret;
	}
}
