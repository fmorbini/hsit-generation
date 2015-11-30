package charniak;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.XMLUtils;

public class CharniakXML {
	public static final String RESULTID="result";
	public static final String PARSEID="parse";
	public static final String SCOREID="score";
	public static final String RANKID="rank";
	private List<CharniakParserOutput> result=null;
	
	public CharniakXML(String xml) {
		try {
			Document doc = XMLUtils.parseXMLString(xml, true, true);
			Node rootNode = doc.getDocumentElement();
			Queue<Node> q=new LinkedList<Node>();
			NodeList cs = rootNode.getChildNodes();
			for (int i = 0; i < cs.getLength(); i++) q.add(cs.item(i));
			while(!q.isEmpty()) {
				Node c=q.poll();
				NamedNodeMap childAtt = c.getAttributes();
				if (isResultNode(c)) {
					cs = c.getChildNodes();
					for (int i = 0; i < cs.getLength(); i++) q.add(cs.item(i));
				} else if (isParseNode(c)) {
					Double score=getScoreNodeValue(childAtt);
					String parseTree=XMLUtils.getStringContent(c);
					Integer rank=getRankNodeValue(childAtt);
					if (score==null) score=-1d;
					if (rank==null) rank=(result!=null)?result.size():0;
					CharniakParserOutput p=new CharniakParserOutput(parseTree, score,rank);
					if (result==null) result=new ArrayList<CharniakParserOutput>();
					result.add(p);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isResultNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && n.getNodeName().toLowerCase().equals(RESULTID);
	}
	public static boolean isParseNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && n.getNodeName().toLowerCase().equals(PARSEID);
	}
	public static Double getScoreNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(SCOREID);
		if (node!=null) return Double.parseDouble(StringUtils.cleanupSpaces(node.getNodeValue()));
		else return null;
	}
	public static Integer getRankNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(RANKID);
		if (node!=null) return Integer.parseInt(StringUtils.cleanupSpaces(node.getNodeValue()));
		else return null;
	}
	
	public List<CharniakParserOutput> getResult() {
		return result;
	}
	public CharniakParserOutput getResult(int rank) {
		List<CharniakParserOutput> rs=getResult();
		if (rs!=null) {
			for(CharniakParserOutput r:rs) {
				if (r!=null && r.getRank()==rank) return r;
			}
		}
		return null;
	}


}
