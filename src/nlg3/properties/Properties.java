package nlg3.properties;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Properties extends HashSet<Property> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String UNKNOWN="unknown";
	public static final String TRUE="true";
	public static final String FALSE="false";

	public static final String pnameOBJECTCONTROL="objectControl";
	public static final String pnameMALEPROPERTY="male";
	public static final String pnameFEMALEPROPERTY="female";
	public static final String pnameAGENTPROPERTY="agent";
	
	private static final Set<String> allProperties=new HashSet<String>(); 
	
	public Properties() {
		super();
	}
	public Properties(Properties ps) {
		this();
		if (ps!=null) {
			for(Property p:ps) {
				add(p);
			}
		}
	}

	@Override
	public Properties clone() {
		return new Properties(this);
	}

	public String get(String p) {
		Property x=getPropertyNamed(p);
		return x.getValue();
	}

	public Property getPropertyNamed(String p) {
		for(Property x:this) {
			if (x.getName().equals(p)) return x;
		}
		return new Property(p,Properties.UNKNOWN);
	}
	
	public boolean hasPropertyValue(String name,String value) {
		Property p=getPropertyNamed(name);
		return p.getValue().equalsIgnoreCase(value);
	}
	
	public boolean retainAll(Collection<?> c) {
		System.err.println("intersection of properties is not meaningfull;");
		return super.retainAll(c);
		/*
		boolean changed=false;
		Iterator<Property> it=iterator();
		while(it.hasNext()) {
			Property p=it.next();
			if (c==null || !c.contains(p.getName())) {
				changed=true;
				it.remove();
			}
		}
		return changed;
		*/
	}
	
	@Override
	public boolean add(Property e) {
		Property found=null;
		String eName=e.getName();
		for(Property p:this) {
			if (p.getName().equalsIgnoreCase(eName)) {
				found=p;
				break;
			}
		}
		if (found!=null) found.setValue(e.getValue());
		else {
			allProperties.add(eName);
			super.add(e);
		}
		return true;
	}
	public static Set<String> getAllProperties() {
		return allProperties;
	}
	
	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		ret.append("<");
		boolean first=true;
		for(Property x:this) {
			if (!first) ret.append(" ");
			else first=false;
			ret.append(x.getName()+"="+x.getValue());
		}
		ret.append(">");
		return ret.toString();
	}
	
}
