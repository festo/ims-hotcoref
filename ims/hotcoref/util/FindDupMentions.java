package ims.hotcoref.util;

import ims.util.IntPair;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindDupMentions {

	public static void main(String[] args) throws IOException{
		
		final BufferedReader in;
		if(args.length==0){
			in=new BufferedReader(new InputStreamReader(System.in,"UTF8"));
		} else {
			in=new BufferedReader(new InputStreamReader(new FileInputStream(args[0]),"UTF8"));
		}
		doit(in);
	}

	private static void doit(BufferedReader in) throws IOException {
		String line;
		List<String[]> lines=new ArrayList<String[]>();
		int lineCount=0;
		int sentenceBeg=0;
		int docCount=0;
		while((line=in.readLine())!=null){
			lineCount++;
			if(line.startsWith("#")){
				if(line.startsWith("#begin"))
					docCount++;
				continue;
			}
			if(line.matches("^\\s*$")){
				String[] corefCol = extractCorefCol(lines, sentenceBeg);
				List<IntPair> problems=check(corefCol);
				lines.clear();
				for(IntPair ip:problems){
					int lineBeg=ip.i1+sentenceBeg;
					int lineEnd=ip.i2+sentenceBeg;
					System.out.println("Problem with mention on lines ("+lineBeg+","+lineEnd+"), doc "+docCount);
				}
				if(!wellFormed(corefCol))
					System.out.println("Not wellformed on sentence beginning on line "+sentenceBeg);
				continue;
			}
			if(lines.isEmpty())
				sentenceBeg=lineCount;
			lines.add(line.split("\\s+"));
		}
	}

	
    private static boolean wellFormed(String[] corefCol) {
    	int count=0;
    	for(String s:corefCol){
    		for(int i=0;i<s.length();++i){
    			if(s.charAt(i)=='(')
    				count++;
    			else if(s.charAt(i)==')')
    				count--;
    			if(count<0)
    				return false;
    		}
    	}
    	return true;
	}


	private static final Pattern BAR=Pattern.compile("\\|");
    private static final Pattern ONE_TOKEN=Pattern.compile("\\((\\d+)\\)");
    private static final Pattern BEGIN=Pattern.compile("\\((\\d+)");
    private static final Pattern END=Pattern.compile("(\\d+)\\)");
    private static final Pattern BLANK=Pattern.compile("(?:\\*|-|_)");
	
    
    
	static Pattern SINGLE_TOKEN_PATTERN=Pattern.compile("\\(\\d+\\)");
	private static List<IntPair> check(String[] corefCol) {
		if(corefCol.length==0)
			return Collections.emptyList();
		List<List<String>> entries=new ArrayList<List<String>>();
		List<IntPair> problems=new ArrayList<IntPair>();
		for(int i=0;i<corefCol.length;++i){
			final List<String> add;
			if(BLANK.matcher(corefCol[i]).matches()){
				add=Collections.emptyList();
			} else if(corefCol[i].contains("|")){ //more than one
				String[] a=BAR.split(corefCol[i]);
				add=new ArrayList<String>();
				boolean seenSingleTokenMention=false;
				for(String s:a){
					if(ONE_TOKEN.matcher(s).matches()){
						if(seenSingleTokenMention)
							problems.add(new IntPair(i,i));
						seenSingleTokenMention=true;
					} else {
						add.add(s);
					}
				}
			} else {
				if(SINGLE_TOKEN_PATTERN.matcher(corefCol[i]).matches()){
					add=Collections.emptyList();
				} else {
					add=Arrays.asList(corefCol[i]);
				}
			}
			entries.add(add);
		}
		//Ok, now we have separated out opening and closing tags into the lists in entries
		Map<Integer,List<Integer>> m=new TreeMap<Integer,List<Integer>>(); //This maps CorefIDs to a list of start tokens
		Set<IntPair> covered=new HashSet<IntPair>();
		for(int i=0;i<entries.size();++i){
			List<String> el=entries.get(i);
			for(String e:el){
				Matcher m2=BEGIN.matcher(e);
				if(m2.matches()){
					Integer chainId=new Integer(m2.group(1));
					if(!m.containsKey(chainId)){
						m.put(chainId, new ArrayList<Integer>());
					}
					m.get(chainId).add(i);
					continue;
				}
                Matcher m3=END.matcher(e);
                if(m3.matches()){
                	Integer chainId=new Integer(m3.group(1));
                	List<Integer> starts=m.get(chainId);
                	int begin=starts.remove(starts.size()-1);
                	int end=i;
                	IntPair ip=new IntPair(begin,end);
                	if(covered.contains(ip)){
                		problems.add(ip);
                	}
                	covered.add(ip);
                }
			}
		}
		return problems;
	}

	private static String[] extractCorefCol(List<String[]> lines,int sentenceBeg) throws Error {
		if(lines.isEmpty())
			return new String[0];
		String[] corefCol=new String[lines.size()];
		int lineLen=lines.get(0).length;
		for(int i=0;i<corefCol.length;++i){
			String[] a=lines.get(i);
			if(a.length!=lineLen)
				throw new Error("inconsistent number of columns on line "+(sentenceBeg+i));
			corefCol[i]=a[lineLen-1];
		}
		return corefCol;
	}
}
