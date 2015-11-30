package nlg3.selection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;

import com.clearnlp.nlp.NLPGetter;
import com.clearnlp.segmentation.AbstractSegmenter;
import com.clearnlp.tokenization.AbstractTokenizer;

import charniak.CharniakFormat;
import charniak.CharniakParserOutput;
import charniak.CharniakXML;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.ProgressTracker;
import edu.usc.ict.nl.util.StringUtils;

public class ParserSelection implements SelectionI {
	
	private PrintStream out;
	private int debug;
	private String baseUrl="http://localhost:8080";
	private AbstractTokenizer tokenizer;
	private AbstractSegmenter segmenter;
	private BufferedWriter pLogFile=null;
	private Double coefficient=null;
	private Double intercept=null;
	public ParserSelection(PrintStream out,int debug,String baseUrl,Double m,Double b) {
		this(out,debug,baseUrl,null,m,b);
	}
	public ParserSelection(PrintStream out,int debug,String baseUrl) {
		this(out,debug,baseUrl,null,null,null);
	}
	public ParserSelection(PrintStream out,int debug,String baseUrl,File pLog,Double m,Double b) {
		tokenizer = NLPGetter.getTokenizer("en");
		segmenter = NLPGetter.getSegmenter("en", tokenizer);
		this.out=out;
		this.debug=debug;
		this.baseUrl=baseUrl;
		if (pLog!=null) {
			try {
				if (pLog.exists()) pLog.delete();
				this.pLogFile=new BufferedWriter(new FileWriter(pLog, true));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.coefficient=m;
		this.intercept=b;
	}

	@Override
	public String selectBest(List<String> candidates,int optiontoPrint) {
		//System.out.println(FunctionalLibrary.printCollection(candidates, "", "", "\n"));
		String r=selectMostProbable(candidates,optiontoPrint);
		return r;
	}
	
	private String selectMostProbable(final List<String> candidates, int optiontoPrint) {
		String best=null;
		int bestIndex=-1;
		int i=1;
		double bestScore = -Double.MIN_VALUE;
		final Map<String,CharniakParserOutput> logProbabilitiesForSentences=new HashMap<String, CharniakParserOutput>();
		if (candidates!=null) {
			//List<Integer> sortedPositions=new ArrayList<Integer>(candidates.size());
			if (candidates.size()>1) {
				ProgressTracker pt = new ProgressTracker(10, candidates.size(), System.out);
				for(String c:candidates) {
					pt.updateDelta(1);
					double p=0;
					//sortedPositions.add(i-1);
					try {
						List<String> sentences = getSentencesSimple(c);
						for(String s:sentences) {
							if (!logProbabilitiesForSentences.containsKey(s)) {
								CharniakParserOutput pr = getTopParse(s);
								logProbabilitiesForSentences.put(s, pr);
							}
						}
						p=getLogProbabilityForList(sentences,logProbabilitiesForSentences,optiontoPrint==i);
						//logProbabilitiesForSentences.put(c, p);
						if (best==null || p>bestScore) {
							best=c;
							bestScore=p;
							bestIndex=i;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (debug>0) out.println(i+": "+p);
					i++;
				}
			} else {
				best=candidates.get(0);
			}
			/*
			Collections.sort(sortedPositions, new Comparator<Integer>() {

				@Override
				public int compare(Integer o1, Integer o2) {
					return (int) Math.signum(probabilitiesForSentences.get(candidates.get(o2))-probabilitiesForSentences.get(candidates.get(o1)));
				}
				
			});
			List<Double> probabilities=new ArrayList<Double>(candidates.size());
			for(int j:sortedPositions) probabilities.add(probabilitiesForSentences.get(candidates.get(j)));
			List<Double> delta = FunctionalLibrary.derivative(probabilities);
			List<Double> maxDelta = FunctionalLibrary.derivative(delta);
			int pos=FunctionalLibrary.findPosMax(maxDelta);
			System.out.println("top choices:");
			for(int j=0;j<=pos-2;j++) {
				System.out.println(sortedPositions.get(j)+": "+candidates.get(sortedPositions.get(j)));
			}
			*/
		}
		out.println("best selected: "+(bestIndex));
		return best;
	}

	private double getLogProbabilityForList(List<String> sentences,Map<String, CharniakParserOutput> logProbabilitiesForSentences, boolean print) {
		if (sentences!=null && logProbabilitiesForSentences!=null) {
			double p=0;
			for(String s:sentences) {
				CharniakParserOutput pr=logProbabilitiesForSentences.get(s);
				double v = pr.getProb();
				double av=adjustProbability(v,pr,s);
				if (print) out.println(s+": "+v+" "+av);
				p+=av;
			}
			return p;
		}
		return 0;
	}
	private Set<String> alreadySaved=null;
	private void savePLog(String s,int x,double y) {
		//if (alreadySaved==null) alreadySaved=new HashSet<>();
		//if (!alreadySaved.contains(s)) {
			//alreadySaved.add(s);
			try {
				pLogFile.write(x+"\t"+y+"\n");
				pLogFile.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		//}
	}
	private Double adjustProbability(Double v,CharniakParserOutput pr,String s) {
		int nt=CharniakFormat.countNonTerminals(CharniakFormat.buildTree(pr.getParse()));
		//int nt=s.split("[\\s]+").length;
		if (coefficient!=null && intercept!=null) {
			v-=coefficient*(double)nt+intercept;
		}
		if (pLogFile!=null) savePLog(s,nt,v);
		return v;
	}
	public List<String> getSentences(String input) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		List<String> ret=null;
		List<List<String>> ss = segmenter.getSentences(new BufferedReader(new StringReader(input)));
		if (ss!=null) {
			for(int i=0;i<ss.size();i++) {
				String sentence=FunctionalLibrary.printCollection(ss.get(i),"",""," ");
				if (!StringUtils.isEmptyString(sentence)) {
					sentence=StringUtils.removeLeadingAndTrailingSpaces(sentence);
					if (ret==null) ret=new ArrayList<String>();
					ret.add(sentence);
				}
			}
		}
		return ret;
	}
	public List<String> getSentencesSimple(String input) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		List<String> ret=null;
		String[] parts=input.split("\\.([\\s]+|$)");
		for(String p:parts) {
			if (ret==null) ret=new ArrayList<String>();
			ret.add(p+".");
		}
		return ret;
	}
	
	public CharniakParserOutput getTopParse(String sentence) {
		CharniakXML response =  new CharniakXML(ClientBuilder.newClient()
				.target(baseUrl).path("charniak/parse").queryParam("sentence", sentence).queryParam("timeout", "20")
				.request().get(String.class));
		//System.out.println(r);
		//System.out.println("    "+response.getResult(0).getProb());
		return response.getResult(0);
	}
	private double getLogProbabilityForParse(CharniakParserOutput tp) {
		if (tp!=null) return tp.getProb();
		return 0;
	}
	private double getLogProbabilityForSentence(String sentence) {
		CharniakParserOutput tp = getTopParse(sentence);
		return getLogProbabilityForParse(tp);
	}

	public static double recomputeInterceptSoAllAreNonPositive(double coefficient,double intercept,File datapoints) throws IOException {
		BufferedReader in=new BufferedReader(new FileReader(datapoints));
		String line=null;
		while((line=in.readLine())!=null) {
			String[] parts=line.split("[\\s]+");
			int x=Integer.parseInt(parts[0]);
			double y=Double.parseDouble(parts[1]);
			double nv=y-(coefficient*(double)x+intercept);
			if (nv>0) {
				intercept+=nv;
				nv=y-(coefficient*(double)x+intercept);
			}
		}
		in.close();
		return intercept;
	}
}
