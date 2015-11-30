package nlg2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lf.pos.NP;
import lf.pos.POS;
import nlg3.properties.Properties;
import nlg3.properties.Property;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.ExcelUtils;

public class NLG2Lexicon {
	public enum ARGTYPE {AGENT,THEME,PATIENT,STIMULUS,DESTINATION,EXPERIENCER,LOCATION,PREDICATE};
	private static final Pattern argsPattern=Pattern.compile("^[\\s]*ARG([0-9]+):[\\s]*([^\\s]+)[\\s]*$");
	private static final Pattern psPattern=Pattern.compile("^[\\s]*([^\\s]+):[\\s]*([^\\s]+)[\\s]*$");
	public class Entry {
		String name;
		String type;
		Map<ARGTYPE,Integer> typeAndPosition;
		List<String> surface;
		String length;
		Properties properties=new Properties();

		public Entry(String n, String t, String a, String properties, List<String> s) {
			this.name=n;
			this.type=t;
			this.surface=s;
			if (!StringUtils.isEmptyString(properties)) {
				String[] ps=properties.split(",");
				for(String p:ps) {
					Matcher m=psPattern.matcher(p);
					if (m.matches()) {
						String pName=m.group(1);
						String pValue=m.group(2);
						if (!StringUtils.isEmptyString(pName)) {
							this.properties.add(new Property(pName,pValue));
						}
					}
				}
				
			}
			if (!StringUtils.isEmptyString(a)) {
				String[] args=a.split(",");
				for(String arg:args) {
					Matcher m=argsPattern.matcher(arg);
					if (m.matches()) {
						String at=m.group(2);
						Integer pos=Integer.parseInt(m.group(1));
						if (typeAndPosition==null) typeAndPosition=new HashMap<NLG2Lexicon.ARGTYPE, Integer>();
						typeAndPosition.put(ARGTYPE.valueOf(at.toUpperCase()), pos);
					}
				}
			}
		}

		public Integer getSubjectPosition() {
			if (typeAndPosition!=null) {
				Integer pos=typeAndPosition.get(ARGTYPE.AGENT);
				if (pos!=null) return pos;
				pos=typeAndPosition.get(ARGTYPE.PATIENT);
				if (pos!=null) return pos;
				pos=typeAndPosition.get(ARGTYPE.EXPERIENCER);
				return pos;
			}
			return null;
		}
		public ARGTYPE getTypeOfArgumentAtPosition(int pos) {
			if (typeAndPosition!=null) {
				for(ARGTYPE a:typeAndPosition.keySet()) {
					int ap=typeAndPosition.get(a);
					if (pos==ap) return a;
				}
			}
			return null;
		}

		public String getType() {
			return type;
		}
		public List<String> getSurface() {
			return surface;
		}
		public String getName() {
			return name;
		}

		public boolean isPassive() {
			return false;
		}

		public Properties getProperties() {
			return properties;
		}
	}
	private Map<String, Entry> lex=null;
	
	public NLG2Lexicon(String pFile) {
		Map<Integer, String> predicates = ExcelUtils.extractRowAndThisColumn(pFile, 0, 0);
		Map<Integer, String> types = ExcelUtils.extractRowAndThisColumn(pFile, 0, 1);
		Map<Integer, String> arguments = ExcelUtils.extractRowAndThisColumn(pFile, 0, 2);
		Map<Integer, String> properties = ExcelUtils.extractRowAndThisColumn(pFile, 0, 3);
		Map<Integer, List<String>> surfaces = ExcelUtils.extractRowsAndColumnWiseData(pFile, 0, 0,4, -1, false, true);
		
		if (predicates!=null && types!=null && arguments!=null && surfaces!=null) {
			for(Integer row:predicates.keySet()) {
				addEntry(predicates.get(row),types.get(row),arguments.get(row),properties.get(row),surfaces.get(row));
			}
		}
	}

	private static final Pattern pName=Pattern.compile("^[\\s]*([^\\s]+)_([0-9]+)[\\s]*$");
	private void addEntry(String pname, String type, String args,String properties,List<String> surfaces) {
		if (lex==null) lex=new HashMap<String, Entry>();
		String name=normalize(pname);
		Entry e=new Entry(name,type,args,properties,surfaces);
		// store both primed and non-primed versions
		lex.put(name, e);
		if (name.endsWith("'")) {
			name=name.substring(0, name.length()-1);
			lex.put(name, e);
		}
	}

	private static String normalize(String in) {
		Matcher m=pName.matcher(in);
		if (m.matches()) {
			return m.group(1).toLowerCase();
		} //else System.err.println("'"+in+"' doesn't match expected argument lexicon format.");
		return in.toLowerCase();
	}

	public String getType(String pname) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) {
				String ret = e.getType();
				if (ret==null) System.err.println("POS information not present for lexical entry: "+pname);
				return ret;
			}
		}
		return null;
	}

	public List<String> getSurface(String pname) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) return e.getSurface();
		}
		return null;
	}
	public List<String> getSurface(String pname,String pos) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) {
				if (!StringUtils.isEmptyString(pos)) {
					String type=e.getType();
					if (!StringUtils.isEmptyString(type) && type.matches(pos)) return e.getSurface();
				} else return e.getSurface();
			}
		}
		return null;
	}
	
	public Properties getProperties(String pname) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			return e.getProperties();
		}
		return new Properties();
	}
	public Properties getProperties(POS thing) {
		return thing.getProperties(this);
	}

	public Integer getSubjectPosition(String pname) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) {
				Integer pos=e.getSubjectPosition();
				if (e.getType()==null) System.err.println("POS information not present for lexical entry: "+pname);
				return pos;
			}
		}
		return null;
	}
	public ARGTYPE getTypeOfArgumentAtPosition(String pname,int pos) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) return e.getTypeOfArgumentAtPosition(pos);
		}
		return null;
	}

	public boolean isPassive(String pname) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) return e.isPassive();
		}
		return false;
	}

	public boolean contains(String pname) {
		if (lex!=null) {
			pname=normalize(pname);
			return lex.containsKey(pname);
		}
		return false;
	}
	
	public static void main(String[] args) {
		NLG2Lexicon lex=new NLG2Lexicon("predicateList.xlsx");
		Properties ps = lex.getProperties(new NP("door'"));
		System.out.println(ps);
	}
}
