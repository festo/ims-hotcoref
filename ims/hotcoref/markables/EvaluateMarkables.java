package ims.hotcoref.markables;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ims.hotcoref.Options;
import ims.hotcoref.data.Chain;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Span;
import ims.hotcoref.io.DocumentReader;
import ims.hotcoref.io.DocumentWriter;
import ims.hotcoref.io.ReaderWriterFactory;
import ims.hotcoref.lang.Language;

public class EvaluateMarkables {

	public static final boolean REMOVE_VERBS=false;
	public static final int PRINT_OUT_TH=100; 
	
	static class CountEntry implements Comparable<CountEntry>{
		final String s;
		int total=0;
		int asMention=0;
		CountEntry(String s){
			this.s=s;
		}
		@Override
		public int compareTo(CountEntry arg0) {
			return arg0.total-this.total;
		}
	}
	
	int tp=0;
	int fp=0;
	int fn=0;
	
	Map<String,CountEntry> counts=new HashMap<String,CountEntry>();
	
	private void addStringCount(String s,boolean isMention){
		CountEntry ce=counts.get(s);
		if(ce==null){
			ce=new CountEntry(s);
			counts.put(s,ce);
		}
		ce.total++;
		if(isMention)
			ce.asMention++;
	}
	
	public void counts(Collection<Span> key,Collection<Span> pred){
		for(Span g:key){
			if(pred.contains(g)){
				addStringCount(g.getSurfaceForm(),true);
				tp++;
			}else{
				fn++;
			}
		}
		for(Span p:pred){
			if(!key.contains(p)){
				addStringCount(p.getSurfaceForm(),false);
				fp++;
			}
		}
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder("Mention extractor stats:\n");
		double p=100.0*tp/(tp+fp);
		double r=100.0*tp/(tp+fn);
		double f=2*p*r/(p+r);
		sb.append(String.format("Precision:  100 * %d / (%d + %d) \t = %.3f\n", tp,tp,fp,p));
		sb.append(String.format("Recall:     100 * %d / (%d + %d) \t = %.3f\n", tp,tp,fn,r));
		sb.append(String.format("F1:         %.3f\n",f));
		sb.append(String.format("Total ext:  %d\n",(tp+fp)));
		return sb.toString();
	}
	
	public static void main(String[] args) throws IOException{
		Options options=new Options(args);
		Language.initLanguage(options.lang);
		EvaluateMarkables eval=new EvaluateMarkables();
		IMarkableExtractor me=MarkableExtractorFactory.getExtractorS(options.markableExtractors==null?Language.getLanguage().getDefaultMarkableExtractors():options.markableExtractors);
		DocumentReader reader=ReaderWriterFactory.getInputReader(options);
		System.out.println("Using markable extractors: "+me.toString());
		if(me.needsTraining()){
			System.out.println("Training markable extractor");
			me.train(reader,options.count);
		}
		DocumentWriter writer=ReaderWriterFactory.getOutputWriter(options);
//		reader=ReaderWriterFactory.getReader(options.inputFormat, new File("/home/users0/anders/corpora/ontonotes5/conll-2012/v3/data/eng_dev_v3_auto_conll"));
		if(options.in2!=null)
			reader=ReaderWriterFactory.getReader(options.inputFormat, options.in2,options.inGz,options.inputEnc);
		
		for(Document d:reader){
			Set<Span> p=me.extractMarkables(d);
			Chain[] m=GoldStandardChainExtractor.getGoldChains(d);
			if(REMOVE_VERBS)
				pruneVerbsFromChains(m);
			Set<Span> g=chains2set(m);
			eval.counts(g, p);
			d.clearCorefCols();
			List<Chain> l=buildSingleTonChain(p);
			d.setCorefCols(l);
			writer.write(d);
		}
		writer.close();
		System.out.println(eval.toString());
		System.out.println();
		eval.printCounts(System.out);
		System.out.println();
		System.out.println("Drop count: "+NonReferentialPruner.dropCount);
		options.done();
	}

	private void printCounts(PrintStream out) {
		List<CountEntry> l=new ArrayList<CountEntry>(counts.values());
		Collections.sort(l);
		for(CountEntry ce:l){
			if(ce.total<PRINT_OUT_TH)
				break;
			double perc=100.0*ce.asMention/ce.total;
			String s=String.format("%-50s %5d %5d %4.2f", ce.s.replaceAll(" ", "_"),ce.total,ce.asMention,perc);
			out.println(s);
		}
		System.out.println("Do:  egrep '[[:digit:]]+ [[:digit:]]+(\\.[[:digit:]]+)?$'  <this-file>  | sort -n -k 4");
	}

	public static Chain buildOneChain(Set<Span> p,Integer id) {
		Chain c=new Chain(id);
		for(Span s:p)
			c.addSpan(s);
		return c;
	}
	
	public static List<Chain> buildSingleTonChain(Set<Span> p){
		List<Chain> chains=new ArrayList<Chain>();
		int i=1;
		for(Span s:p)
			chains.add(new Chain(i++,s));
		return chains;
	}

	public static Set<Span> chains2set(Chain[] chains){
		Set<Span> set=new HashSet<Span>();
		for(Chain c:chains)
			set.addAll(c.spans);
		return set;		
	}

	private static void pruneVerbsFromChains(Chain[] goldChains) {
		for(int i=0;i<goldChains.length;++i){
			Chain c=goldChains[i];
			Iterator<Span> sIt=c.spans.iterator();
			while(sIt.hasNext()){
				Span s=sIt.next();
				if(s.s.tags[s.hd].startsWith("V"))
					sIt.remove();
			}
		}
	}
	
}
