package nlg3.reference;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lf.pos.NP;
import lf.pos.POS;
import nlg3.NLG3Lexicon;
import nlg3.properties.Properties;
import nlg3.properties.Property;

public class ReferenceItem {

	private Properties properties=null;
	private NP original=null;
	private Set<String> unavailableProperties=null;
	private List<Integer> coord=null;
	
	public ReferenceItem() {
	}
	
	public ReferenceItem(NP d,NLG3Lexicon lexicon,List<Integer> coord) {
		Properties ps = lexicon.getProperties(d);
		properties=ps!=null?ps.clone():new Properties();
		this.coord=coord;
		if (d.getMods()!=null) {
			for(POS a:d.getMods()) {
				properties.add(new Property(a.toString(), Properties.TRUE));
			}
		}
		if (!d.isPronoun() && d.getNoun()!=null) properties.add(new Property(d.getNoun(), Properties.TRUE));
		this.original=(NP) d.clone();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj!=null) {
			if (this==obj) return true;
			if (obj!=null && obj instanceof ReferenceItem) {
				for(String pname:Properties.getAllProperties()) {
					String v=getPropertyValue(pname);
					if (!v.equalsIgnoreCase(((ReferenceItem) obj).getPropertyValue(pname))) return false;
				}
				return true;
			}
		}
		return super.equals(obj);
	}
	
	public List<Integer> getCoord() {
		return coord;
	}
	
	public int getPropertiesSize() {
		return properties.size();
	}
	public String getPropertyValue(String p) {
		return properties.get(p);
	}
	private Properties getProperties() {
		return properties;
	}
	
	public NP getOriginal() {
		return original;
	}
	
	@Override
	public String toString() {
		Set<String> ups = getUnavailableProperties();
		Properties ps = getProperties();
		StringBuffer ret=new StringBuffer();
		ret.append("[");
		if (ps!=null) {
			boolean first=true;
			for(Property p:ps) {
				String pname=p.getName()+((ups!=null && ups.contains(p.getName()))?"_TRANSP":"");
				ret.append((first?"":" ")+pname+":"+p.getValue());
				first=false;
			}
		}
		ret.append("]");
		return ret.toString();
	}

	public Property getPropertyNamed(String pname) {
		return getProperties().getPropertyNamed(pname);
	}
	
	@Override
	protected ReferenceItem clone() {
		ReferenceItem ret=new ReferenceItem();
		ret.original=original;
		ret.properties=properties.clone();
		ret.coord=coord;
		return ret;
	}
	
	public void updateProperty(String name,String value) {
		getProperties().add(new Property(name, value));
	}
	
	public boolean isAvailable(String pname) {
		return (unavailableProperties==null || !unavailableProperties.contains(pname));
	}
	public void setAsUnavailable(String pname) {
		if (unavailableProperties==null) unavailableProperties=new HashSet<String>();
		unavailableProperties.add(pname);
	}
	public void setAsAvailable(String pname) {
		if (unavailableProperties!=null && unavailableProperties.contains(pname)) unavailableProperties.remove(pname);
	}
	public boolean hasUnavailableProperties() {
		return unavailableProperties!=null && !unavailableProperties.isEmpty();
	}
	public Set<String> getUnavailableProperties() {
		return unavailableProperties!=null?unavailableProperties:null;
	}

}
