package charniak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import edu.usc.ict.nl.util.graph.Node;

public class CharniakFormat {
	public static Node buildTree(String p) {
		char[] input=p.toCharArray();
		Node r=new Node("root");
		List<Node> cs=buildTrees(0,input);
		if (cs!=null) for(Node cn:cs) {
			try {
				r.addEdgeTo(cn, false, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return r;
	}
	
	private static List<Node> buildTrees(int start, char[] input) {
		List<Node> ret=null;
		for(int i=start;i<input.length;i++) {
			if (input[i]=='(') {
				int c=findClosing('(', ')', i+1, input);
				if (c>0) {
					if (ret==null) ret=new ArrayList<Node>();
					char[] cc=Arrays.copyOfRange(input, i, c+1);
					List<Node> cs = buildTrees(1, cc);
					String name=getNodeName(cc);
					Node n=new Node(name);
					if (cs!=null) for(Node cn:cs) {
						try {
							n.addEdgeTo(cn, false, false);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					ret.add(n);
				}
				i=c+1;
			}
		}
		return ret;
	}

	private static String getNodeName(char[] parseBlock) {
		int start=-1,end=-1;
		for(int i=0;i<parseBlock.length;i++) {
			if (parseBlock[i]=='(') start=i+1;
			if (Character.isWhitespace(parseBlock[i])) end=i;
			if (start>=0 && end>start) return new String(Arrays.copyOfRange(parseBlock, start, end));
		}
		return null;
	}

	public static int findClosing(char open,char close,int start,char[] input) {
		boolean escape=false;
		int levels=1;
		for(int i=start+1;i<input.length;i++) {
			char c=input[i];
			if (c==open) {
				if (!escape) levels++;
				escape=false;
			} else if (c=='\\') {
				escape=!escape;
			} else if (c==close) {
				if (!escape) {
					levels--;
					if (levels==0) {
						return i;
					}
				}
				escape=false;
			} else escape=false;
		}
		return -1;
	}
	
	public static int countNonTerminals(Node input) {
		int ret=0;
		if (input!=null) {
			Set<Node> nodes = input.getAllNodes();
			if (nodes!=null) {
				for(Node n:nodes) {
					if (n.hasChildren()) ret++;
				}
			}
		}
		return ret;
	}
	
	public static void main(String[] args) {
		String p="(S1 (S (NP (DT The) (NN circle)) (VP (VBZ creeps) (PRT (RP up)) (PP (IN on) (NP (DT the) (JJ big) (NNP triangle))) (SBAR (IN because) (S (NP (DT the) (NN circle)) (VP (VBZ wants) (SBAR (IN that) (S (NP (DT the) (JJ big) (NN triangle)) (VP (VBZ does) (RB not) (VP (VB see) (NP (DT the) (NN circle)))))))))) (. .)))";
		Node r=buildTree(p);
		r.toGDLGraph("test.gdl");
		System.out.println(countNonTerminals(r));
	}
}
