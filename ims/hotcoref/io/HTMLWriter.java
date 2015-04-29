package ims.hotcoref.io;

import ims.hotcoref.data.Chain;
import ims.hotcoref.data.CorefSolution;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.markables.GoldStandardChainExtractor;
import ims.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLWriter implements DocumentWriter {

	private final BufferedWriter out;

	
	private static final String STYLESHEET=
		"<style type=\"text/css\">\n"+
		"  .c0 {background-color:#CCCC66}\n"+
		"  .c1 {background-color:#CC66CC}\n"+
		"  .c2 {background-color:#66CCCC}\n"+
		"  .c3 {background-color:#CC6666}\n"+
		"  .c4 {background-color:#66CC66}\n"+
		"  .c5 {background-color:#6666CC}\n"+
		"  .c6 {background-color:#AA6666}\n"+
		"  .c7 {background-color:#66AA66}\n"+
		"  .c8 {background-color:#6666AA}\n"+
		"  .c9 {background-color:#AAAA66}\n"+
		"  .c10 {background-color:#AA66AA}\n"+
		"  .c11 {background-color:#66AAAA}\n"+
		"  .c12 {background-color:#CCAA66}\n"+
		"  .c13 {background-color:#CC66AA}\n"+
		"  .c14 {background-color:#66CCAA}\n"+
		"  .c15 {background-color:#AACC66}\n"+
		"  .c16 {background-color:#66AACC}\n"+
		"  .c17 {background-color:#AA66CC}\n"+
		"</style>\n";
	
	
	public HTMLWriter(File out) throws IOException {
		this.out=Util.getWriter(out);
		writerHeader();
	}
	
	@Override
	public synchronized void close() throws IOException {
		writerFooter();
		out.close();
	}

	@Override
	public synchronized void write(Document d) throws IOException {
		out.write("<h3>"+d.header+"</h3>\n");
		out.write("<div>\n");
		out.write("<table cellpadding=2 cellspacing=2>\n");
//		out.write("<ol start=\"0\">");
//		CorefSolution cs=gsce.getGoldCorefSolution(d);
		CorefSolution cs=GoldStandardChainExtractor.getGoldCorefSolution(d);
		Map<Integer,String> ssClassMap=getClassMap(cs.getKey());
		int sentId=0;
		for(Sentence s:d.sen){
			StringBuilder sb=new StringBuilder();
			for(int i=1;i<s.forms.length;++i){
				sb.append(getTokenBegin(s,i,ssClassMap));
				sb.append(s.forms[i]);
				sb.append(getTokenEnd(s,i));
				sb.append(' ');
			}
//			out.write("<li>");
			out.write("<tr><td>"+sentId+"</td>");
			out.write(getSpeaker(s));
			out.write("<td>"+sb.toString()+"</td></tr>");
//			out.write("</li>");
//			out.write("<br/>\n");
			sentId++;
		}
//		out.write("</ol>");
		out.write("</table>\n");
		out.write("</div>\n");
		out.flush();
	}

	private String getSpeaker(Sentence s) {
		return "<td><tt>"+s.speaker[1]+"&nbsp;&nbsp;&nbsp;&nbsp</tt></td>";
	}

	private Map<Integer, String> getClassMap(List<Chain> key) {
		Map<Integer,String> m=new HashMap<Integer,String>();
		int i=0;
		for(Chain c:key){
			i%=18;
			m.put(c.chainId, "c"+i);
			i++;
		}
		return m;
	}

	private static final Pattern BEGIN_PATTERN=Pattern.compile("\\((\\d+)");
	private String getTokenBegin(Sentence s, int i, Map<Integer, String> ssClassMap) {
		Matcher m=BEGIN_PATTERN.matcher(s.corefCol[i]);
		StringBuilder sb=new StringBuilder();
		while(m.find()){
			sb.append("<span class=\"").append(ssClassMap.get(Integer.parseInt(m.group(1)))).append("\">");
			sb.append("<sup>").append(m.group(1)).append("</sup>");
			sb.append("[<sub>").append(i).append("</sub>");
		}
		return sb.toString();
	}
	
	private static final Pattern END_PATTERN=Pattern.compile("(\\d+)\\)");
	private String getTokenEnd(Sentence s, int i) {
		Matcher m=END_PATTERN.matcher(s.corefCol[i]);
		StringBuilder sb=new StringBuilder();
		while(m.find()){
			sb.append("<sub>").append(i).append("</sub>");
			sb.append("]<sup>").append(m.group(1)).append("</sup>");
			sb.append("</span>");
		}
		return sb.toString();
	}

	private void writerFooter() throws IOException {
		out.write("</body></html>\n");
		out.flush();
	}

	private void writerHeader() throws IOException {
		out.write("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /><title>coref out</title>\n");
		out.write(STYLESHEET);
		out.write("</head><body>\n");
		out.flush();
	}
	
}
