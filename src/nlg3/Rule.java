package nlg3;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nlg3.reference.ReferenceItem;
import nlg3.reference.ReferenceStack;
import lf.pos.POS;

public class Rule {
	private SyntaxBuilder trigger;
	private List<SyntaxBuilder> builders;
	
	public Rule(SyntaxBuilder p) {
		this.trigger=p;
	}
	
	public void addBuilder(SyntaxBuilder b) {
		if (builders==null) builders=new ArrayList<SyntaxBuilder>();
		builders.add(b);
	}
	
	public boolean evaluate(POS thing,TraversalState state) throws Exception {
		Object r=trigger.generateSyntaxRoot(thing,state,null,true);
		if (r!=null && r instanceof Boolean) return ((Boolean)r);
		else throw new Exception("trigger didn't return a boolean: "+r);
	}
	
	public List<SyntaxBuilder> getBuilders() {
		return builders;
	}

	public List<SyntaxBuilder> getBuildersForGroup(int group) {
		List<SyntaxBuilder> ret=null;
		List<SyntaxBuilder> bs = getBuilders();
		if (bs!=null) {
			for(SyntaxBuilder b:bs) {
				if (b.isInGroup(group)) {
					if (ret==null) ret=new ArrayList<SyntaxBuilder>();
					ret.add(b);
				}
			}
		}
		return ret;
	}
	public boolean hasBuilderInGroup(int group) {
		List<SyntaxBuilder> bs = getBuilders();
		if (group<0) return true;
		else if (bs!=null) for(SyntaxBuilder b:bs) if (b.isInGroup(group)) return true;
		return false;
	}
	public boolean hasBuilderAtRow(int row) {
		List<SyntaxBuilder> bs = getBuilders();
		if (bs!=null) for(SyntaxBuilder b:bs) if (b.getRow()==row) return true;
		return false;
	}
	
	public String getID() {return (trigger!=null)?trigger.toString():null;}
}
