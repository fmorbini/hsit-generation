package lf.pos;

import java.util.LinkedList;
import java.util.List;

import nlg2.NLG2Lexicon;
import nlg3.properties.Properties;


public abstract class POS implements POSI {
	
	@Override
	public void put(List<Integer> coord, POS el) throws Exception {
		if (coord==null || coord.isEmpty()) System.err.println("cannot put at null coord.");
		else {
			POS parent=this;
			int i=0;
			for(;i<coord.size()-1;i++) {
				parent=parent.getChildren().get(coord.get(i));
			}
			parent.updateChild(coord.get(i), el);
		}
	}
	
	public static LinkedList<Integer> getParentCoord(List<Integer> coord) {
		if (coord!=null && coord.size()>=0) {
			if (coord.isEmpty()) return new LinkedList<Integer>();
			else return new LinkedList<Integer>(coord.subList(0, coord.size()-1));
		}
		return null;
	}
	public static POS getParent(LinkedList<Integer> c,POS root) {
		LinkedList<Integer> parentCoord=POS.getParentCoord(c);
		if (parentCoord!=null) {
			POS thing = root.getElementAt(parentCoord);
			return thing;
		}
		return null;
	}
	public POS getElementAt(List<Integer> coord) {
		if (coord==null || coord.isEmpty()) return this;
		else {
			POS parent=this;
			int i=0;
			for(;i<coord.size()-1;i++) {
				parent=parent.getChildren().get(coord.get(i));
			}
			return parent.getChildren().get(coord.get(i));
		}
	}

	public static boolean isThisAParentOfThat(List<Integer> parent, List<Integer> child) {
		int lp=parent!=null?parent.size():0;
		int lc=child!=null?child.size():0;
		if (lp<lc) {
			for(int i=0;i<lp;i++) if (child.get(i)!=parent.get(i)) return false;
			return true;
		}
		return false;
	}

	@Override
	public POS clone() {
		return null;
	}
	
	@Override
	public void updateChild(int pos, POS child) throws Exception {
		getChildren().set(pos, child);
	}

	/**
	 * given a coord that identifies a node, (null or empty identifies the root node) return the next node to be visited for
	 * a depth first traversal of the tree ruted at this.
	 * NOTE: coord is modified.
	 * @param coord
	 * @return
	 */
	public POS getNextItem(LinkedList<Integer> coord) {
		//first check if there are children to the currently selected node.
		POS current=getElementAt(coord);
		List<POS> cs=current.getChildren();
		if (cs!=null && !cs.isEmpty()) {
			for(int i=0;i<cs.size();i++) {
				POS child=cs.get(i);
				if (child!=null) {
					coord.addLast(i);
					return child;
				}
			}
		}
		do {
			// if not, then backtrack until you find the next available node to visit.
			int last=(coord.isEmpty())?-1:coord.removeLast();
			POS parent=getElementAt(coord);
			cs = parent.getChildren();
			if (cs!=null && cs.size()>last+1) {
				for(int lasti=last+1;lasti<cs.size();lasti++) {
					POS child=cs.get(last+1);
					if (child!=null) {
						coord.addLast(last+1);
						return child;
					}
				}
			}
		} while (!coord.isEmpty());
		return null;
	}

	@Override
	public Properties getProperties(NLG2Lexicon lex) {
		return null;
	}

	@Override
	public boolean isPronoun() {
		return false;
	}
	@Override
	public boolean isReflexivePronoun() {
		return false;
	}
}
