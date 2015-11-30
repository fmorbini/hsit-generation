package nlg3.properties;


public class Property {
	
	private String name;
	private String value;
	
	public Property(String pname, String pvalue) {
		this.name=pname;
		if (pvalue==null || !(pvalue.equalsIgnoreCase(Properties.TRUE) || pvalue.equalsIgnoreCase(Properties.FALSE) || pvalue.equalsIgnoreCase(Properties.UNKNOWN))) {
			System.err.println("invalid property value: "+pvalue);
		}
		this.value=pvalue;
	}

	public enum PROPERTIES {GENDER}

	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	public boolean isSubsumedBy(String otherValue) {
		return this.value==otherValue || (this.value!=null && this.value.equalsIgnoreCase(otherValue));
	}
}
