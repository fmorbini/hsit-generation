package nlg2;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lf.NLUUtils;
import lf.Predication;
import lf.graph.NLUGraphUtils;

import org.w3c.tools.sexpr.Symbol;

import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Node;

public class NLG2Data {

	public final List<Predication> clauses;
	public final Node logicForm;
	public final Map<String, Symbol> equalities;
	public final Map<Predication, Node> explanations;
	public final Map<String,Set<Object>> eventToLiteral;
	public final List<Node> obs;

	public NLG2Data(List<Predication> independentLiteralsOrder, Node lf,Map<String, Symbol> groupsOfEqualities,Map<Predication, Node> associatedExplanations, List<Node> obs) {
		this.clauses=independentLiteralsOrder;
		this.logicForm=lf;
		this.equalities=groupsOfEqualities;
		this.explanations=associatedExplanations;
		this.eventToLiteral=NLUGraphUtils.getEventsFromLF(lf);
		this.obs=obs;
	}

	public Set<String> getEventualityThatareObservations() {
		return getEventualityThatareObservations(obs);
	}
	public static Set<String> getEventualityThatareObservations(List<Node> obs) {
		Set<String> ret=null;
		if (obs!=null) {
			for(Node o:obs) {
				Object nlu=NLUUtils.parse(o.getName(), false,false);
				String ev=NLUUtils.getEventualityName(nlu);
				if (!StringUtils.isEmptyString(ev)) {
					if (ret==null) ret=new HashSet<String>();
					ret.add(ev);
				}
			}
		}
		return ret;
	}

}
