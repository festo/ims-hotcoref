package ims.hotcoref.markables;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ims.hotcoref.Options;
import ims.hotcoref.data.CorefSolution;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Span;
import ims.hotcoref.io.DocumentReader;
import ims.hotcoref.io.ReaderWriterFactory;
import ims.hotcoref.lang.Language;
import ims.util.MutableInt;

public class EvaluteNERMarkables {
	
	public static void main(String[] args) throws IOException{
		Options options=new Options(args);
		Language.initLanguage(options.lang);
		DocumentReader reader=ReaderWriterFactory.getInputReader(options);
		AbstractMarkableExtractor me=new AbstractNERExtractor.AllNERExtractor();
		IMarkableExtractor standardME=MarkableExtractorFactory.getExtractorS(options.markableExtractors==null?Language.getLanguage().getDefaultMarkableExtractors():options.markableExtractors);
		
		int goldMentions=0;
		int standardExtr=0;
		int neExtr=0;
		
		int diff=0;
		
		TreeMap<String,MutableInt> neCounts=new TreeMap<String,MutableInt>();
		TreeMap<String,MutableInt> diffNeCounts=new TreeMap<String,MutableInt>();
		TreeMap<String,MutableInt> isMention=new TreeMap<String,MutableInt>();
		TreeMap<String,MutableInt> caughtByStandard=new TreeMap<String,MutableInt>();
		
		int docCount=0;
		for(Document d:reader){
			docCount++;
			CorefSolution cs=GoldStandardChainExtractor.getGoldCorefSolution(d);
			Set<Span> gold=cs.getClonedSpanSet();
			Set<Span> standard=standardME.extractMarkables(d);
			Set<Span> nes=me.extractMarkables(d);
			goldMentions+=gold.size();
			standardExtr+=standard.size();
			neExtr+=nes.size();
			Set<Span> nesInGoldButNotInStandard=difference(intersection(gold,nes),standard);
			diff+=nesInGoldButNotInStandard.size();
			
			for(Span s:nes){
				addToMap(neCounts,s.ne.getLabel());
				if(gold.contains(s))
					addToMap(isMention,s.ne.getLabel());
				if(standard.contains(s))
					addToMap(caughtByStandard,s.ne.getLabel());
			}
			for(Span s:nesInGoldButNotInStandard)
				addToMap(diffNeCounts,s.ne.getLabel());
		}
		
		System.out.println(docCount + " documents");
		System.out.println("Gold mentions:  "+goldMentions);
		System.out.println("Standard extr:  "+standardExtr);
		System.out.println("NE extr:        "+neExtr);
		System.out.println();
		System.out.println("diff count: "+diff);
		System.out.println();
		System.out.println("NEs broken down");
		System.out.printf("%-13s  %6s %6s %6s %6s\n","Label","Total","Diff","IsMent","Caught");
		int sum1=0;
		int sum2=0;
		int sum3=0;
		int sum4=0;
		for(String label:neCounts.keySet()){
			int total=neCounts.get(label).getValue();
			int d=diffNeCounts.containsKey(label)?diffNeCounts.get(label).getValue():0;
			int e=isMention.containsKey(label)?isMention.get(label).getValue():0;
			int f=caughtByStandard.containsKey(label)?caughtByStandard.get(label).getValue():0;
			sum1+=total;
			sum2+=d;
			sum3+=e;
			sum4+=f;
			System.out.printf("%-13s  %6d %6d %6d %6d\n",label,total,d,e,f);
		}
		System.out.printf("%-13s  %6d %6d %6d %6d\n","sum",sum1,sum2,sum3,sum4);
	}

	public static Set<Span> intersection(Set<Span> s1,Set<Span> s2){
		Set<Span> copy=new HashSet<Span>(s1);
		copy.retainAll(s2);
		return copy;
	}
	
	public static Set<Span> difference(Set<Span> s1,Set<Span> s2){
		Set<Span> copy=new HashSet<Span>(s1);
		copy.removeAll(s2);
		return copy;
	}
	
	public static <T> void addToMap(Map<T,MutableInt> tm, T key){
		MutableInt mi=tm.get(key);
		if(mi==null){
			tm.put(key,new MutableInt(1));
		} else {
			mi.increment();
		}
	}
}
