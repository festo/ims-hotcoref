package ims.hotcoref.data;

import ims.hotcoref.symbols.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Document {

	public final String header;
	public String footer;
	public final List<Sentence> sen;
	public final String genre;
	public final String docName;
	public final String docNo;
	public Map<Span,Integer> stackMap;
	
	public Document(String header) {
		this.header=header;
		this.sen=new ArrayList<Sentence>();
		if(header==null){
			this.genre=null;
			this.docName=null;
			this.docNo=null;
		} else {
			this.genre=getGenre(header);
			this.docName=getDocNameString(header);
			this.docNo=getDocNoString(header);
		}
	}

	public void clearCorefCols(){
		for(Sentence s:sen)
			s.clearCorefCol();
	}

//	public void setCorefCols(Map<Integer, Chain> chainMap) {
//		for(Entry<Integer,Chain> e:chainMap.entrySet()){
//			for(Span s:e.getValue().spans){
//				s.s.addCoref(s,e.getKey());
//			}
//		}
//	}
	
	public void setCorefCols(List<Chain> chains) {
		for(Chain c:chains){
			for(Span s:c.spans){
				s.s.addCoref(s,c.chainId);
			}
		}
	}
	
	public void addSentence(Sentence s){
		int quoteCount=0;
		if(sen.size()>0){
			Sentence prevSentence=sen.get(sen.size()-1);
			quoteCount=prevSentence.quoteCount[prevSentence.forms.length-1];
		}
		for(int i=1;i<s.forms.length;++i){
			s.quoteCount[i]=quoteCount;
			if(s.forms[i].equals("\""))
				quoteCount++;
		}
		sen.add(s);
	}
	
	public void setFooter(String footer){
		this.footer=footer;
	}

	
	private static final Pattern GENRE_HEADER_PATTERN=Pattern.compile("^#begin document \\(([a-z]+)\\/.*$");
	static String getGenre(String header){
		Matcher m=GENRE_HEADER_PATTERN.matcher(header);
		if(m.matches()){
			return m.group(1);
		} else {
			return "<unknown>";
		}
	}
	static final Pattern DOCNOPATTERN=Pattern.compile("^#begin document .*?; part (\\d+)$");
	static String getDocNoString(String header) {
		Matcher m=DOCNOPATTERN.matcher(header);
		if(m.matches()){
			return Integer.toString(Integer.parseInt(m.group(1)));
		} else {
			throw new RuntimeException("Invalid document header. Failed to parse doc number in "+header);
		}
	}
	static final Pattern DOCNAMEPATTERN=Pattern.compile("^#begin document \\((.*?)\\); part \\d+$");
	static String getDocNameString(String header) {
		Matcher m=DOCNAMEPATTERN.matcher(header);
		if(m.matches()){
			return m.group(1);
		} else {
			throw new RuntimeException("Invalid document header. Failed to parse doc name in "+header);
		}
	}
	
	public Span getSpanFromUniqueKey(int key){
		int end=Span.MAX_KEY_PART_M1&key;
		key>>=10;
		int start=Span.MAX_KEY_PART_M1&key;
		key>>=10;
		int s=Span.MAX_KEY_PART_M1&key;
		return sen.get(s).getSpan(start, end);
	}

	public void initSInst(SymbolTable symbolTable) {
		for(Sentence s:sen)
			s.initInst(symbolTable);
	}
}
