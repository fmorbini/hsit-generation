package charniak;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.util.StringUtils;

public class CharniakParserReader extends Thread {

	private BufferedReader is;
	public CharniakParserReader(InputStream inputStream) {
		super("parser reader");
		this.is=new BufferedReader(new InputStreamReader(inputStream));
		start();
	}

	private enum STATE {LOOKING,READING,DONE};
	private static final Pattern countLine=Pattern.compile("[\\s]*([0-9]+)[\\s]+[0-9]+[\\s]*");
	private STATE state=STATE.LOOKING;
	private List<CharniakParserOutput> result=null;
	
	@Override
	public void run() {
		if (result!=null) result.clear();
		String line;
		int number=0;
		Float score=null;
		String parse=null;
		try {
			while((line=is.readLine())!=null) {
				//System.out.println(state+": "+line);
				//System.out.println("result: "+result);
				if (!StringUtils.isEmptyString(line)) {
					switch (state) {
					case DONE:
						result=null;
					case LOOKING:
						Matcher m=countLine.matcher(line);
						if (m.matches()) {
							state=STATE.READING;
							number=Integer.parseInt(m.group(1));
							score=null;
							parse=null;
						}
						break;
					case READING:
						if (number>0) {
							try {
								score=Float.parseFloat(line);
							} catch (Exception e) {
								parse=line;
							}
							if (parse!=null) {
								if (result==null) result=new ArrayList<CharniakParserOutput>();
								result.add(new CharniakParserOutput(parse, score,result.size()));
								number--;
								parse=null;
								score=null;
							}
						}
						if (number<=0) state=STATE.DONE;
					}
				} else {
					if (result==null) result=new ArrayList<CharniakParserOutput>();
					if (result.isEmpty()) {
						result.add(new CharniakParserOutput(parse, score!=null?score:0,result.size()));
					}
					parse=null;
					score=null;
					state=STATE.DONE;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isDone() {return state==STATE.DONE;}
	
	public List<CharniakParserOutput> getResult() {
		return result;
	}

	public void invalidateResult() {
		state=STATE.LOOKING;
		result=null;
	}

}
