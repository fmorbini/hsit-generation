package charniak;


public class CharniakParserOutput {

	private double prob;
	private String parse;
	private int rank;

	public CharniakParserOutput(String output,double prob,int rank) throws Exception {
		this.parse=output;
		this.prob=prob;
		this.rank=rank;
	}
	
	@Override
	public String toString() {
		return getProb()+"("+getRank()+"): "+getParse();
	}
	
	public double getProb() {
		return prob;
	}
	public String getParse() {
		return parse;
	}
	
	public int getRank() {
		return rank;
	}

}
