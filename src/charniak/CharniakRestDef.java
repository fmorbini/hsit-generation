package charniak;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
 
@Path("/charniak/")
public class CharniakRestDef {
 
	static Map<String,List<CharniakParserOutput>> cache=new LinkedHashMap<String,List<CharniakParserOutput>>(10000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String,List<CharniakParserOutput>> eldest) {
            return size() > 10000;
        }
    };
	
    @GET
    @Path("/parse/")
    @Produces(MediaType.TEXT_XML)
    public String parse(@QueryParam("sentence") String sentence,@QueryParam("timeout") Integer timeout) {
        StringBuffer ret=new StringBuffer();
        ret.append("<?xml version=\"1.0\"?>" + "<"+CharniakXML.RESULTID+">" );
        if (timeout==null || timeout<1) timeout=10;
		try {
			List<CharniakParserOutput> result=(cache!=null)?cache.get(sentence):null;
			boolean fromcache=result!=null;
			if (fromcache) System.out.println("retrieving parsing resultfor '"+sentence+"' from cache.");
			else System.out.println("parsing sentence '"+sentence+"' with timeout of "+timeout+" second(s).");
			if (result==null) result = Server.parser.parse(sentence, timeout);
			if (!fromcache && cache!=null && result!=null) cache.put(sentence,result);
			//System.out.println("server result: "+result);
			if (result!=null) {
				for(CharniakParserOutput po:result) {
					ret.append("<"+CharniakXML.PARSEID+" "+CharniakXML.SCOREID+"=\""+po.getProb()+"\" "+CharniakXML.RANKID+"=\""+po.getRank()+"\">\n");
					ret.append(po.getParse()+"\n");
					ret.append("</"+CharniakXML.PARSEID+">\n");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		ret.append("</"+CharniakXML.RESULTID+">");
		return ret.toString();
    }
    
    @GET
    @Path("/reset/")
    @Produces(MediaType.TEXT_XML)
    public String reset() {
    	int size=cache.size();
    	cache.clear();
    	int sizeAfter=cache.size();
        StringBuffer ret=new StringBuffer();
        ret.append("<?xml version=\"1.0\"?>" + "<"+CharniakXML.RESULTID+" before=\""+size+"\" after=\""+sizeAfter+"\">" );
		ret.append("</"+CharniakXML.RESULTID+">");
		return ret.toString();
    }
}