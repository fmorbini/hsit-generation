package nlg3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lf.pos.POS;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;

public class POSwT {

	List<SyntaxBuilder> builders;
	List<POS> after;
	List<List<Integer>> coords;
	POS start;
	
	public POSwT(POS start) {
		this.start=start;
	}
	
	public void addRuleAndResult(SyntaxBuilder b,POS result,List<Integer> coord) {
		if (builders==null) builders=new ArrayList<SyntaxBuilder>();
		builders.add(b);
		if (after==null) after=new ArrayList<POS>();
		after.add(result);
		if (coords==null) coords=new ArrayList<List<Integer>>();
		coords.add(coord);
		assert(after.size()==builders.size());
		assert(after.size()==coords.size());
	}
	
	public POS getLast() {
		if (after!=null && !after.isEmpty()) return after.get(after.size()-1);
		else return start;
	}
	
	public NLGElement toSimpleNLG(NLGFactory nlgFactory) {
		POS last=getLast();
		return (last!=null)?last.toSimpleNLG(nlgFactory):null;
	}

	public boolean equals(POS obj) {
		POS last=getLast();
		return (last!=null)?last.equals(obj):false;
	}

	public List<POS> getChildren() {
		POS last=getLast();
		return (last!=null)?last.getChildren():null;
	}
	
	@Override
	protected POSwT clone() throws CloneNotSupportedException {
		POSwT ret=new POSwT(this.start);
		if (this.after!=null) ret.after=new ArrayList<POS>(this.after);
		if (this.builders!=null) ret.builders=new ArrayList<SyntaxBuilder>(this.builders);
		if (this.coords!=null) ret.coords=new ArrayList<List<Integer>>(this.coords);
		return ret;
	}
	
	@Override
	public String toString() {
		StringBuffer b=new StringBuffer();
		b.append("/===\n");
		if (start!=null) b.append(start+"\n");
		if (builders!=null) {
			Iterator<SyntaxBuilder> it1=builders.iterator();
			Iterator<POS> it2=after.iterator();
			Iterator<List<Integer>> it3=coords.iterator();
			String ind=" ";
			while(it1.hasNext()) {
				SyntaxBuilder bu=it1.next();
				POS a=it2.next();
				List<Integer> c=it3.next();
				b.append(ind+c+bu+"\n");
				b.append(ind+a+"\n");
				ind+=" ";
			}
		}
		b.append("\\===\n");
		return b.toString();
	}
}
