package charniak;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread.State;
import java.util.Arrays;
import java.util.List;

import edu.usc.ict.nl.util.StreamGobbler;
import edu.usc.ict.nl.util.StringUtils;

public class CharniakParser {
	private OutputStream stdin=null;
	private Process p;
	private InputStream stdout;
	private CharniakParserReader reader=null;
	private int nBest;

	public CharniakParser(int nBest) {
		this.nBest=nBest;
		try {
			start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void start() throws Exception {
		List<String> command = Arrays.asList("C:\\cygwin\\bin\\bash", "-c", "PATH=$PATH:/bin:/usr/local/bin;first-stage/PARSE/parseIt.exe -l399 -N"+nBest+" first-stage/DATA/SANCL2012-Uniform/parser/");
		//List<String> command = Arrays.asList("C:\\cygwin\\bin\\bash", "-c", "PATH=$PATH:/bin:/usr/local/bin;first-stage/PARSE/parseIt.exe -l399 -N"+nBest+" first-stage/DATA/EN/");
		//System.out.println(FunctionalLibrary.printCollection(command, "", "", " "));
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(new File("C:\\Users\\morbini\\bllip-parser\\").getAbsoluteFile());
		//pb.environment().putAll(env);
		System.out.println(System.currentTimeMillis()+": starting parser: "+command);
		p = pb.start();
		stdout = p.getInputStream();
		StreamGobbler error = new StreamGobbler(p.getErrorStream(), "error",true,false);
		error.start();
		stdin=p.getOutputStream();
	}

	public List<CharniakParserOutput> parse(String sentence,int timeout) throws Exception {
		if (stdin!=null) {
			sentence=StringUtils.removeLeadingAndTrailingSpaces(sentence);
			if (!sentence.startsWith("<s>") && !sentence.startsWith("<S>")) sentence="<s> "+sentence;
			if (!sentence.endsWith("</s>") && !sentence.endsWith("</S>")) sentence=sentence+" </s>";
			if (reader==null || reader.getState()!=State.RUNNABLE) {
				System.err.println("Restarting the reader. state="+(reader!=null?reader.getState():null));
				reader = new CharniakParserReader(stdout);
			}
			stdin.write(sentence.getBytes());
			stdin.write("\n".getBytes());
			stdin.flush();
			reader.invalidateResult();
			long startTime=System.currentTimeMillis();
			boolean done=false;
			while(System.currentTimeMillis()-startTime<timeout*1000 && !done) {
				done=reader.isDone();
				Thread.sleep(100);
			}
			if (!done) {
				kill();
				start();
			} else {
				return reader.getResult();
			}
		}
		return null;
	}

	public void kill() {
		System.out.println("killing parser.");
		if (p!=null) {
			if (stdin!=null)
				try {
					stdin.close();
				} catch (IOException e) {}
			if (stdout!=null)
				try {
					stdout.close();
				} catch (IOException e) {}
			p.destroy();
			reader=null;
		}
	}
	
	public static void main(String[] args) throws Exception {
		CharniakParser p = new CharniakParser(10);
		List<CharniakParserOutput> result = p.parse("The circle creeps up on the big triangle because the circle wants that the big triangle does not see the circle.",120);
		System.out.println(result);
		result = p.parse("The circle creeps up on the big triangle because the circle wants the big triangle to not see the circle.",40);
		System.out.println(result);
		p.kill();
	}
}
