package ims.hotcoref.util;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import ims.hotcoref.Options;
import ims.hotcoref.data.CorefSolution;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Span;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.io.DocumentReader;
import ims.hotcoref.io.ReaderWriterFactory;
import ims.hotcoref.lang.Language;
import ims.hotcoref.markables.GoldStandardChainExtractor;
import ims.hotcoref.markables.IMarkableExtractor;
import ims.hotcoref.markables.MarkableExtractorFactory;
import ims.util.DBO;
import ims.util.MutableInt;
import ims.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class EntityGridHTML {

	private static final boolean DIAG_ARROWS=true;
	private static final boolean MENTIONS_HEADS_ONLY=false;
	private static final boolean MENTIONS_HORIZONTAL=true;
	private static boolean IGNORE_SINGLETONS=true;
	
	//http://stackoverflow.com/questions/272799/vertical-rotated-text-in-html-table
	private static final String CSS_TEMPLATE=
			"<style type=\"text/css\">\n" +
			".box_rotate {\n" +
			"  -moz-transform: rotate(7.5deg);  /* FF3.5+ */\n" +
			"  -o-transform: rotate(7.5deg);  /* Opera 10.5 */\n" +
			"  -webkit-transform: rotate(7.5deg);  /* Saf3.1+, Chrome */\n" +
			"  filter:  progid:DXImageTransform.Microsoft.BasicImage(rotation=0.083);  /* IE6,IE7 */\n" +
			"  -ms-filter: \"progid:DXImageTransform.Microsoft.BasicImage(rotation=0.083)\"; /* IE8 */\n" +
			"}\n" +
			"td { background-color: #CCCCCC; }\n" +
			".singleton { background-color: #EEEEEE; }\n" +
			".pronoun { background-color: #FFCCCC; }\n" +
			".common  { background-color: #CCFFCC; }\n" +
			".proper  { background-color: #CCCCFF; }\n" +
			".pronoun, .common, .proper { text-decoration: underline; cursor: pointer; }\n" +
			".highlightedrow td { background-color: #CC6666; }\n" +
			"</style>\n";
	
	private static final String JS_HEADER=
			"<script language=\"JavaScript\">\n" +
			" var highlightedRowId=-1;\n" +
			"\n" +
			"function prhl(rowid){\n" +
			"  if (highlightedRowId!=-1) { document.getElementById(highlightedRowId).className=''; }\n" +
			"  highlightedRowId=rowid;\n" +
			"  document.getElementById(rowid).className=\"highlightedrow\";\n" +
			"}\n" +
			"</script>\n";

	private final Map<String,CountEntry> paths;
	private final List<CountEntry> pathList;
	
	public EntityGridHTML(DocumentReader reader,IMarkableExtractor me){
		paths=registerPaths(reader,me);
		pathList=new ArrayList<CountEntry>(paths.values());
		Collections.sort(pathList);
	}
	
	private Map<String, CountEntry> registerPaths(DocumentReader reader,IMarkableExtractor me){
		DBO.println("Registering paths");
		DBO.printNoPrefix("Doc: ");
		Map<String,MutableInt> counts=new TreeMap<String,MutableInt>();
		Map<String,MutableInt> corefCounts=new TreeMap<String,MutableInt>();
		int erase=0;
		int docCount=0;
		for(Document d:reader){
			++docCount;
			erase=DBO.eraseAndPrint(erase, docCount);
			CorefSolution gold=GoldStandardChainExtractor.getGoldCorefSolution(d);
			Map<Span,Integer> sp2int=gold.getSpan2IntMap();
			Set<Span> s=me.extractMarkables(d);
			List<Span> l=new ArrayList<Span>(s);
			Collections.sort(l);
			for(Span sp:l){
				String path=getRootPath(sp);
				Util.incrementMapValue(counts, path);
				if(sp2int.containsKey(sp))
					Util.incrementMapValue(corefCounts, path);
			}
		}
		DBO.println();
		List<Entry<String,MutableInt>> q=new ArrayList<Entry<String,MutableInt>>();
		q.addAll(counts.entrySet());
		Comparator<Entry<String,MutableInt>> cmp=new Comparator<Entry<String,MutableInt>>(){
			@Override
			public int compare(Entry<String, MutableInt> arg0,Entry<String, MutableInt> arg1) {
				MutableInt mi0=arg0.getValue();
				MutableInt mi1=arg1.getValue();
				if(mi0.getValue()>mi1.getValue())
					return -1;
				if(mi0.getValue()<mi1.getValue())
					return 1;
				return 0;
			}
		};
		Collections.sort(q,cmp);
//		Map<String,Integer> map=new HashMap<String,Integer>();
		Map<String,CountEntry> map=new HashMap<String,CountEntry>();
		int idx=1;
		for(Entry<String,MutableInt> e:q){
			MutableInt corefCount=corefCounts.get(e.getKey());
			map.put(e.getKey(), new CountEntry(e.getValue().getValue(),corefCount==null?0:corefCount.getValue(),idx++,e.getKey()));
		}
		
		return map;
	}

	private String getRootPath(Span sp) {
		CFGNode n=sp.cfgNode;
		StringBuilder sb=new StringBuilder();
		if(n==null){
			n=sp.s.ct.getMinimalIncludingNode(sp.start, sp.end);
			sb.append("NONE^^").append(n.getLabel());
		} else {
			sb.append(n.getLabel());
		}
		CFGNode last=n;
		for(n=n.getParent();n!=null && !n.getLabel().equals("DUMMY");last=n,n=n.getParent()){
			if(DIAG_ARROWS){
				int lHead=last.getHead();
				int curHead=n.getHead();
				if(lHead==curHead)
					sb.append("^^");
				else if(lHead<curHead)
					sb.append(">^");
				else
					sb.append("<^");
			} else {
				sb.append("^^");
			}
			sb.append(n.getLabel());
		}
		return sb.toString();
	}
	
	static class CountEntry implements Comparable<CountEntry>{
		final int total;
		final int coref;
		final int idx;
		final String path;
		public CountEntry(int total, int coref, int idx,String path) {
			this.total = total;
			this.coref = coref;
			this.idx = idx;
			this.path=path;
		}
		@Override
		public int compareTo(CountEntry arg0) {
			return this.idx-arg0.idx;
		}
	}

	
	private void dumpPaths(File file,Collection<CountEntry> countEntries) throws IOException {
		BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"utf8"));
		writer.write("<html><head><title>Mention paths</title></head><body>\n");
		dumpPathTable(countEntries, writer,null);
		writer.write("</body></html>\n");
		writer.close();
	}

	private void dumpPathTable(Collection<CountEntry> countEntries,BufferedWriter writer,TIntIntHashMap localCounts) throws IOException {
		writer.write("<table id=\"pathtable\"><tr><td>Index</td><td>Path</td><td>Coref Count</td><td>Total Count</td><td>Index</td>");
		if(localCounts!=null)
			writer.write("<td>In-document counts</td>");
		writer.write("</tr>\n");
		for(CountEntry ce:countEntries)
			writer.write(getTableRow(ce,localCounts));
		writer.write("</table>\n");
	}
	
	
	//Arrows: http://www.fileformat.info/info/unicode/category/So/list.htm
	private String getTableRow(CountEntry ce, TIntIntHashMap localCounts) {
		StringBuilder sb=new StringBuilder("<tr id=\"").append("pathrow").append(ce.idx).append("\">");
		sb.append("<td>").append(ce.idx).append("</td>");
		sb.append("<td>").append(ce.path.replaceAll("\\>\\^", "&#x2197;").replaceAll("\\<\\^", "&#x2196;").replaceAll("\\^\\^", "&uarr;")).append("</td>");
		sb.append("<td>").append(ce.coref).append("</td>");
		sb.append("<td>").append(ce.total).append("</td>");
		sb.append("<td>").append(ce.idx).append("</td>");
		if(localCounts!=null)
			sb.append("<td>").append(localCounts.get(ce.idx)).append("</td>");
		//sb.append("<td>").append("").append("</td>");
		sb.append("</tr>\n");
		return sb.toString();
	}
	
	private void dumpGrids(DocumentReader reader, File graphOutDir,IMarkableExtractor me) throws IOException {
		DBO.println("Dumping grids");
		DBO.printNoPrefix("Doc: ");
		int docCount=0;
		int erase=0;
		for(Document d:reader){
			++docCount;
			erase=DBO.eraseAndPrint(erase, docCount);
			CorefSolution cs=GoldStandardChainExtractor.getGoldCorefSolution(d);
			List<Span> spans=new ArrayList<Span>();
			Set<Span> extracted=me.extractMarkables(d);
			Set<Span> goldSpans=cs.getSpanSet();
			TIntIntHashMap localCounts=new TIntIntHashMap();
			if(IGNORE_SINGLETONS){
				for(Span g:goldSpans)
					if(extracted.contains(g))
						spans.add(g);
			} else {
				spans.addAll(extracted);
			}
			Collections.sort(spans);
			for(Span s:spans){
				int pathIdx=paths.get(getRootPath(s)).idx;
				localCounts.adjustOrPutValue(pathIdx, 1, 1);
			}
				
			
			File out=new File(graphOutDir,docCount+".eg.html");
			BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out),"utf8"));
			writer.write("<html><head><title>Entity grid</title>\n");
			writer.write(CSS_TEMPLATE);
			writer.write(JS_HEADER);
			writer.write("</head><body>\n");
			writer.write("<h3>"+d.header+"</h3>\n<hr/>\n");
			dumpEGTable(writer,spans,cs,d);
			writer.write("\n<hr/>\n");
			Set<CountEntry> countEntries=new TreeSet<CountEntry>();
			for(Span s:spans){
				CountEntry ce=paths.get(getRootPath(s));
				if(ce!=null) //shouldn't happen, but just to be on the same side
					countEntries.add(ce);
			}
			dumpPathTable(countEntries,writer,localCounts);
			
			writer.write("</body></html>\n");
			writer.close();
		}
	}

	private void dumpEGTable(BufferedWriter writer, List<Span> spans,CorefSolution cs,Document d) throws IOException {
		//the table is sentences * chains

		//we start by figuring out how many "chains" there are and what their 'representative' mention is.
		Map<Span,Integer> goldSpan2Int=cs.getSpan2IntMap();
		int[] span2Chain=new int[spans.size()];
		List<Span> chainRepresentatives=new ArrayList<Span>(); //these are the representatives
		Map<Integer,Integer> goldChainIDtoLocalIdx=new HashMap<Integer,Integer>();
		List<Boolean> isSingleton=new ArrayList<Boolean>();
		int localChainCounter=0;
		for(int i=0;i<spans.size();++i){
			Span sp=spans.get(i);
			Integer goldCID=goldSpan2Int.get(sp);
			//now look if this is already mapped to a local index
			boolean singleton=goldCID==null;
			boolean increment=(singleton || !goldChainIDtoLocalIdx.containsKey(goldCID));
			if(increment){
				isSingleton.add(singleton?true:false);
				span2Chain[i]=localChainCounter;
				chainRepresentatives.add(sp);
				if(goldCID!=null)
					goldChainIDtoLocalIdx.put(goldCID, localChainCounter);
				++localChainCounter;
			} else { //check if we need to replace the representative
				     //we then know that goldChainIDtoLocalIDx should map onto a local chain index
				int thisSpanLocalChainIdx=goldChainIDtoLocalIdx.get(goldCID);
				span2Chain[i]=thisSpanLocalChainIdx;
				Span curRep=chainRepresentatives.get(thisSpanLocalChainIdx);
				if(!curRep.isProperName){ //if the rep is already a proper name, do nothing
					if(sp.isProperName || (curRep.isPronoun && !sp.isPronoun)) //replace
						chainRepresentatives.set(thisSpanLocalChainIdx, sp);
				}
			}
		}
		//populate this based on chain representatives
		List<String> chain2name=new ArrayList<String>();
		for(Span sp:chainRepresentatives){
			String name=MENTIONS_HEADS_ONLY?sp.s.forms[sp.hd]:sp.getSurfaceForm();
			chain2name.add(name);
		}
		
		//Now figure out what goes in every cell in the table
		TableEntry[][] table=new TableEntry[d.sen.size()][chainRepresentatives.size()];
//		int[][][] values=new int[d.sen.size()][chainRepresentatives.size()][];
//		String[][][] types=new String[d.sen.size()][chainRepresentatives.size()][];
		for(int i=0;i<spans.size();++i){
			Span sp=spans.get(i);
//			int pathIdx=paths.get(getRootPath(sp)).idx;
			int col=span2Chain[i];
//			append(values,sp.s.sentenceIndex,col,pathIdx,types,sp);
			if(table[sp.s.sentenceIndex][col]==null)
				table[sp.s.sentenceIndex][col]=new TableEntry(sp);
			else
				table[sp.s.sentenceIndex][col].append(sp);
		}
		
		writer.write("<table cellpadding=2 cellspacing=2>\n");
		if(MENTIONS_HORIZONTAL){
			//header
			writer.write("<tr><td></td>");
			for(String m:chain2name)
				writer.write("<td class=\"box_rotate\">"+m+"</td>");
			writer.write("<td></td></tr>\n");
			for(int i=0;i<d.sen.size();++i){
				writer.write("<tr><td>"+i+"</td>");
				for(int j=0;j<chainRepresentatives.size();++j){
					String td=(isSingleton.get(j)?"<td class=\"singleton\">":"<td>");
					writer.write(td+(table[i][j]==null?"&nbsp;":table[i][j].toHTMLSpan())+"</td>");
				}

				writer.write("<td>"+i+"</td></tr>\n");
			}
		} else {
			throw new Error("not implemented");
		}
		writer.write("</table>\n");
//		writer.write("<br/>LEGEND<br/>\n");
		writer.write("<br/>");
		writer.write("<span class=\"proper\">Proper name</span>\n");
		writer.write("<span class=\"common\">Common NP</span>\n");
		writer.write("<span class=\"pronoun\">Pronoun</span>\n");
		writer.write("<br/><br/>\n");
	}
	
	class TableEntry {
		TIntArrayList pathsList=new TIntArrayList();
		List<String> spanClasses=new ArrayList<String>();
		List<String> surfaceForms=new ArrayList<String>();
		public TableEntry(Span sp){
			append(sp);
		}
		public void append(Span sp){
			int path=paths.get(getRootPath(sp)).idx;
			String type;
			if(sp.isPronoun)
				type="pronoun";
			else if(sp.isProperName)
				type="proper";
			else
				type="common";
			pathsList.add(path);
			spanClasses.add(type);
			surfaceForms.add(sp.getSurfaceForm());
		}
		public String toHTMLSpan(){
			StringBuilder sb=new StringBuilder("[");
			for(int i=0;i<pathsList.size();++i){
				String pathrowId="pathrow"+pathsList.get(i);
				sb.append("<span onClick=\"prhl('"+pathrowId+"')\" class=\"").append(spanClasses.get(i)).append("\" title=\"").append(surfaceForms.get(i)).append("\">");
				sb.append(pathsList.get(i));
				sb.append("</span>");
				sb.append(",");
			}
			return sb.replace(sb.length()-1, sb.length(), "]").toString();
		}
	}

//	private String join(int[] is, String[] strings) {
//		StringBuilder sb=new StringBuilder("[");
//		for(int i=0;i<is.length;++i){
//			sb.append("<span class=\""+strings[i]+"\">").append(is[i]).append("</span>").append(",");
//		}
//		return sb.replace(sb.length()-1, sb.length(), "]").toString();
//	}
//
//	private void append(int[][][] values, int sentenceIndex, int i, int pathIdx, String[][][] types, Span sp) {
//		String type;
//		if(sp.isPronoun)
//			type="pronoun";
//		else if(sp.isProperName)
//			type="proper";
//		else
//			type="common";
//		
//		if(values[sentenceIndex][i]==null){
//			values[sentenceIndex][i]=new int[]{pathIdx};
//			types[sentenceIndex][i]=new String[]{type};
//		} else {
//			int[] a=values[sentenceIndex][i];
//			String[] b=types[sentenceIndex][i];
//			values[sentenceIndex][i]=new int[a.length+1];
//			types[sentenceIndex][i]=new String[b.length+1];
//			for(int p=0;p<a.length;++p){
//				values[sentenceIndex][i][p]=a[p];
//				types[sentenceIndex][i][p]=b[p];
//			}
//			values[sentenceIndex][i][a.length]=pathIdx;
//			types[sentenceIndex][i][b.length]=type;
//		}
//	}

	public static void main(String[] args) throws IOException{
		Options options=new Options(args);
		IGNORE_SINGLETONS=options.ignoreSingletons;
		if(options.graphOutDir==null)
			throw new RuntimeException("need to specify graph out dir");
		Language.initLanguage(options.lang);
		WordNetInterface.theInstance();
		DocumentReader reader=ReaderWriterFactory.getInputReader(options);
		IMarkableExtractor me=MarkableExtractorFactory.getExtractorS(options);
		if(me.needsTraining())
			me.train(reader, options.count);
		EntityGridHTML egWriter=new EntityGridHTML(reader,me);
		DBO.println();
		DBO.println("Dumping all paths to");
		DBO.println(options.graphOutDir.toString());
		egWriter.dumpPaths(new File(options.graphOutDir,"paths.html"),egWriter.pathList);
		DBO.println();
		egWriter.dumpGrids(reader,options.graphOutDir,me);
		DBO.println();
		options.done();
	}
}
