package nlg3;

import java.io.StringReader;

import nlg2.NLG2Lexicon;
import nlg3.parser.Parser;

public class SyntaxBuilderParser {
	public static void main(String[] args) throws Exception {
		
/*		
		COPANLG2 nlg = new COPANLG2("predicateList.xlsx");
		File prb=new File("paetc/prbs/prb-1-proof.dot");
		List<POS> r=nlg.getSyntax(prb);
		System.out.println(nlg.processSyntax(r));
		System.out.println(r);
*/
		
		new NLG2Lexicon("predicateList.xlsx");
		String input="sentence(subject(.),want,subject(object(.)),infinitive(v(object(.))))";
		Parser parser = new Parser(new StringReader(input));
		Object r2 = parser.syntax();
		System.out.println(r2);
	}
}
