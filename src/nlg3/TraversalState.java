package nlg3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import nlg3.reference.ReferenceItem;
import nlg3.reference.ReferenceStack;
import lf.pos.NP;
import lf.pos.POS;

public class TraversalState {
	public POSwT root;
	public LinkedList<Integer> coord;
	public NLG3Lexicon lex;
	private Set<String> unavailableProperties=null;
	private ReferenceStack references=new ReferenceStack();

	public int getCurrentPosition() {
		return (coord!=null && !coord.isEmpty())?coord.getLast():-1;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		TraversalState ret=new TraversalState();
		ret.root=root;
		ret.coord=coord;
		ret.lex=lex;
		ret.references=references.clone();
		ret.unavailableProperties=unavailableProperties!=null?new HashSet<String>(unavailableProperties):null;
		return ret;
	}

	public boolean isAvailable(String pname) {
		return (unavailableProperties!=null && unavailableProperties.contains(pname));
	}
	public void setAsUnavailable(String pname) {
		if (unavailableProperties==null) unavailableProperties=new HashSet<String>();
		unavailableProperties.add(pname);
	}
	private void setAsAvailable(String pname) {
		if (unavailableProperties!=null && unavailableProperties.contains(pname)) unavailableProperties.remove(pname);
	}
	public boolean hasUnavailableProperties() {
		return unavailableProperties!=null && !unavailableProperties.isEmpty();
	}
	public Set<String> getUnavailableProperties() {
		return unavailableProperties!=null?unavailableProperties:null;
	}

	public ReferenceStack getReferencesForState(LinkedList<Integer> coord) {
		if (references==null) return null;
		else {
			boolean copy=false;
			for(ReferenceItem c:references) {
				if (POS.isThisAParentOfThat(c.getCoord(),coord)) {
					copy=true;
					break;
				}
			}
			if (copy) {
				ReferenceStack ret=new ReferenceStack();
				for(ReferenceItem c:references) {
					if (!POS.isThisAParentOfThat(c.getCoord(),coord)) ret.add(c);
				}
				return ret;
			}
			return references;
		}
	}

	public void updateReferencesWithCurrentPosition() {
		POS d=root.getLast().getElementAt(coord);
		if (d!=null && d instanceof NP && !d.isPronoun()) {
			Iterator<ReferenceItem> it=references.iterator();
			ReferenceItem dri=new ReferenceItem((NP) d,lex,new LinkedList<Integer>(coord));
			Set<String> ups=null;
			boolean update=false;
			while(it.hasNext()) {
				ReferenceItem t=it.next();
				if (t.equals(dri)) {
					it.remove();
					ups=t.getUnavailableProperties();
					update=true;
				}
			}
			if (update && ups!=null) for(String up:ups) dri.setAsUnavailable(up);
			else if (!update && hasUnavailableProperties()) for(String up:getUnavailableProperties()) dri.setAsUnavailable(up);
			references.push(dri);
			}
	}

	public void createEmptyReferenceStack() {
		references=new ReferenceStack();
	}

	public void clearReferences() {
		if (references!=null) references.clear();
	}

}
