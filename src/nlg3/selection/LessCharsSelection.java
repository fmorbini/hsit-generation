package nlg3.selection;

import java.util.List;

import edu.usc.ict.nl.util.StringUtils;

public class LessCharsSelection implements SelectionI {

	@Override
	public String selectBest(List<String> candidates,int optionToPrint) {
		int bestLength=0;
		String best=null;
		if (candidates!=null) {
			for(String c:candidates) {
				String tmp=StringUtils.cleanupSpaces(c);
				int ns=tmp.length();
				if (best==null || bestLength>ns) {
					bestLength=ns;
					best=c;
				}
			}
		}
		return best;
	}

	public int countChar(String s, char n) {
		int count = 0;
		for (int i=0;i<s.length();i++) if (s.charAt(i)==n) count++;
		return count;
	}
}
