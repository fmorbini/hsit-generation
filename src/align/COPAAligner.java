package align;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.tools.sexpr.Cons;
import org.w3c.tools.sexpr.SimpleSExprStream;
import org.w3c.tools.sexpr.Symbol;

import com.clearnlp.dependency.DEPTree;
import com.clearnlp.nlp.NLPGetter;
import com.clearnlp.segmentation.AbstractSegmenter;
import com.clearnlp.tokenization.AbstractTokenizer;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.clearnlp.DepNLU;
import edu.usc.ict.nl.nlu.fst.train.Aligner;
import edu.usc.ict.nl.nlu.fst.train.Alignment;
import edu.usc.ict.nl.nlu.fst.train.AlignmentSummary;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StreamGobbler;


public class COPAAligner extends Aligner {

	private DepNLU parser;
	public COPAAligner(File outputDir) throws Exception {
		super(outputDir);
		parser=new DepNLU();
	}


	public void createTDfromCOPAFiles(File questions,File narratives) throws Exception {
		List<TrainingDataFormat> tds=createNLUTDfromCOPAQuestions(questions);
		List<TrainingDataFormat> tds2=createNLUTDfromCOPANarratives(narratives);
		if (tds!=null && tds2!=null) tds.addAll(tds2);
		else if (tds2!=null) tds=tds2;
		enrichInputWords(tds);
		transformNLURepresentation(tds);
		prepareTDforAligner(tds, in, out);
	}
	
	private void enrichInputWords(List<TrainingDataFormat> tds) {
		if (tds!=null) {
			for(TrainingDataFormat td:tds) {
				String u=td.getUtterance();
				List<DEPTree> result;
				try {
					result = parser.parse(u, System.out);
					String nu=parser.enrichedInputString(result,"");
					td.setUtterance(nu);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}





	private static void transformNLURepresentation(List<TrainingDataFormat> tds) {
		if (tds!=null) {
			StringBuffer nlum=new StringBuffer();
			for (TrainingDataFormat td:tds) {
				nlum.setLength(0);
				String nlu = td.getLabel();
				SimpleSExprStream si = new SimpleSExprStream(new ByteArrayInputStream(nlu.getBytes(StandardCharsets.UTF_8)));
				try {
					Object obj = si.parse();
					simplify(obj,nlum);
					nlu=nlum.toString();
					td.setLabel(nlu);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}


	private static void simplify(Object obj, StringBuffer nlum) throws Exception {
		if (obj instanceof Cons) {
			Enumeration en=((Cons) obj).elements();
			boolean first=true;
			while(en.hasMoreElements()) {
				Object el=en.nextElement();
				if (first) {
					first=false;
					if (!(el instanceof Symbol) || !((Symbol)el).toString().equalsIgnoreCase("and")) {
						simplify(el,nlum);
					}
				} else {
					simplify(el,nlum);
				}
			}
		} else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			SimpleSExprStream.printExpr(obj, ps);
			nlum.append((nlum.length()==0?"":" ")+baos.toString("UTF8"));
		}
	}


	private static List<TrainingDataFormat> createNLUTDfromCOPANarratives(File narratives) {
		return null;
	}

	
	private static final Pattern prb=Pattern.compile(".*[\\d]+\\.[\\s]*(.*)[\\s]*[\n\r]+[\\s]*(.*)[\\s]*[\n\r]+a\\.[\\s]*(.*)[\\s]*[\n\r]+[\\s]*(.*)[\\s]*[\n\r]+b\\.[\\s]*(.*)[\\s]*[\n\r]+[\\s]*(.*)[\\s]*[\n\r]+");
	public static List<TrainingDataFormat> createNLUTDfromCOPAQuestions(File questions) throws Exception {
		List<TrainingDataFormat> ret=null;
		BufferedReader in=new BufferedReader(new FileReader(questions));
		StringBuffer b=new StringBuffer();
		String line;
		int counter=0;
		while((line=in.readLine())!=null) {
			b.append(line+"\n");
			Matcher m=prb.matcher(b.toString());
			if(m.find()) {
				counter++;
				b.setLength(0);
				if (ret==null) ret=new ArrayList<TrainingDataFormat>();
				String sentence=m.group(1);
				AbstractTokenizer tokenizer = NLPGetter.getTokenizer("en");
				AbstractSegmenter segmenter = NLPGetter.getSegmenter("en", tokenizer);
				List<List<String>> ss = segmenter.getSentences(new BufferedReader(new StringReader(sentence)));
				String nsentence="";
				for(int i=0;i<ss.size()-1;i++) {
					nsentence+=(nsentence.length()==0?"":" ")+FunctionalLibrary.printCollection(ss.get(i),"",""," ");
				}
				TrainingDataFormat td=new TrainingDataFormat(nsentence, m.group(2));
				td.setId(sentence);
				ret.add(td);
			}
		}
		System.out.println(counter);
		in.close();
		return ret;
	}
}
