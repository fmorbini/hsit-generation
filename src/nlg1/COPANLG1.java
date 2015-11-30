package nlg1;

import java.io.File;
import java.util.List;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import lf.NLUUtils;
import lf.Predication;
import lf.pos.Sentence;
import nlg1.graph.EventNode;
import nlg1.sorting.AfterInTimeMeansAfterInOrder;
import nlg1.sorting.NeighborsShareObjectAndPredicate;
import nlg1.sorting.NeighborsShareSubject;

public class COPANLG1 {

	private MacroPlanner macro;
	private MicroPlanner micro;
	private Realizer realizer;

	public COPANLG1() {
		macro=new MacroPlanner();
		micro=new MicroPlanner();
		realizer=new Realizer();
	}

	public String process(String nlu) {
		try {
			Object nluObj=NLUUtils.parse(nlu, true,true);
			EventNode tg=macro.process(nluObj,"macro.gdl");
			List<Predication> predicateOrder=micro.process(tg,"micro.gdl",new AfterInTimeMeansAfterInOrder(10),new NeighborsShareSubject(1),new NeighborsShareObjectAndPredicate(0.1f));
			List<Sentence> order=micro.generateSyntax(predicateOrder);
			return realizer.process((List)order);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
