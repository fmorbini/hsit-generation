package nlg1.graph;

import java.util.ArrayList;
import java.util.List;

import edu.usc.ict.nl.util.graph.Node;
import lf.Predication;

public class EventNode extends Node {

	List<Predication> ps=null;
	boolean knownTimeRelation=false;
	
	public EventNode(String id,boolean knownTimeRelation) {
		super(id);
		this.knownTimeRelation=knownTimeRelation;
	}
	
	public void addPredication(Object p) {
		if (ps==null) ps=new ArrayList<Predication>();
		ps.add(new Predication(p));
	}

	public boolean getKnownTimeRelation() {return knownTimeRelation;}
	
	@Override
	public String gdlText() {
		return "node: { shape: "+getShape()+" title: \""+getID()+"\" label: \""+toString()+"\" info1: \""+toString(true)+"\"}\n";
	}

	public String toString() {
		return toString(false);
	}
	public String toString(boolean more) {
		if (more) {
			StringBuffer ret=new StringBuffer();
			ret.append(super.toString());
			if(ps!=null && !ps.isEmpty()) {
				ret.append(":");
				for(Predication p:ps) {
					ret.append(" |"+p.toString()+"|");
				}
			}
			return ret.toString();
		}
		else return super.toString();
	}
	
	
	public List<Predication> getPredications() {
		return ps;
	}
	public void setPredications(List<Predication> ps) {
		this.ps = ps;
	}
}
