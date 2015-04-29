package ims.hotcoref.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import ims.hotcoref.Options;
import ims.hotcoref.data.Chain;
import ims.hotcoref.data.CorefSolution;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.io.DocumentReader;
import ims.hotcoref.io.ReaderWriterFactory;
import ims.hotcoref.lang.Language;
import ims.hotcoref.markables.GoldStandardChainExtractor;
import ims.util.DBO;

public class ChainStats {

	public static void main(String[] args) throws IOException{
		Options options=new Options(args);
		Language.initLanguage(options.lang);
		WordNetInterface.theInstance();
		DocumentReader predReader=ReaderWriterFactory.getInputReader(options);
		DocumentReader goldReader=ReaderWriterFactory.getReader(options.inputFormat, options.gold, options.gold.toString().endsWith(".gz"), options.inputEnc);
		
		ChainStats cs=new ChainStats();
		cs.doIt(goldReader,predReader);
		cs.printStats();
		
	}

	private void printStats() {
		System.out.println("Mentions:");
		System.out.printf("Gold:    %6d\n",totalGMentions);
		System.out.printf("Pred:    %6d\n",totalPMentions);
		System.out.println();
		System.out.println("Chains:  ");
		System.out.printf("Gold:    %6d    (%5.1f mentions/chain)\n",totalGChains,((float) totalGMentions/totalGChains));
		System.out.printf("Pred:    %6d    (%5.1f mentions/chain)\n",totalPChains,((float) totalPMentions/totalPChains));
		System.out.printf("Corr:    %6d\n",totalCorrChains);
		System.out.println();
		System.out.println("Chain size histogram");
		System.out.println("        GOLD      PRED    CORR     PCORRSUBSET     PCORRSUPSET");
		System.out.println();
		TIntHashSet s=new TIntHashSet();
		s.addAll(gChainSizeHisto.keySet());
		s.addAll(pChainSizeHisto.keySet());
		int[] keys=s.toArray();
		Arrays.sort(keys);
		for(int k:keys){
			System.out.printf("%-3d     %-5d     %-5d    %-5d      %-5d         %-5d\n",
					k,
					gChainSizeHisto.containsKey(k)?gChainSizeHisto.get(k):0,
					pChainSizeHisto.containsKey(k)?pChainSizeHisto.get(k):0,
					corrChainSizeHisto.containsKey(k)?corrChainSizeHisto.get(k):0,
					subSetCorrChainSizeHisto.containsKey(k)?subSetCorrChainSizeHisto.get(k):0,
					superSetCorrChainSizeHisto.containsKey(k)?superSetCorrChainSizeHisto.get(k):0);		
		}
	}

	private void doIt(DocumentReader goldReader, DocumentReader predReader) {
		DBO.println("Counting");
		DBO.printWithPrefix("Doc: ");
		Iterator<Document> gIt=goldReader.iterator();
		Iterator<Document> pIt=predReader.iterator();
		int docCount=0;
		int erase=0;
		while(gIt.hasNext()){
			if(!pIt.hasNext())
				throw new RuntimeException("Predicted file ended prematurely at doc "+docCount);
			docCount++;
			erase=DBO.eraseAndPrint(erase, Integer.toString(docCount));
			Document gold=gIt.next();
			Document pred=pIt.next();
			count(gold,pred);
		}
		System.out.println();
		System.out.println();
		if(pIt.hasNext())
			System.err.println("!!!   Reached EOF in gold before done with pred.");
	}

	int totalGMentions=0;
	int totalPMentions=0;
	int totalGChains=0;
	int totalPChains=0;
	int totalCorrChains=0;
	int totalSubSetCorrChains=0;
	int totalSuperSetCorrChains=0;
	TIntIntHashMap gChainSizeHisto=new TIntIntHashMap();
	TIntIntHashMap pChainSizeHisto=new TIntIntHashMap();
	TIntIntHashMap corrChainSizeHisto=new TIntIntHashMap();
	TIntIntHashMap subSetCorrChainSizeHisto=new TIntIntHashMap();
	TIntIntHashMap superSetCorrChainSizeHisto=new TIntIntHashMap();
	
	private void count(Document gold, Document pred) {
		if(!sameDoc(gold,pred))
			throw new Error("Not same document!!");
		Chain[] gcss=GoldStandardChainExtractor.getGoldChains(gold);
		Chain[] pcss=GoldStandardChainExtractor.getGoldChains(pred);
		Arrays.sort(gcss,CHAIN_FIRST_MENTION_COMPARATOR);
		Arrays.sort(pcss,CHAIN_FIRST_MENTION_COMPARATOR);
		totalGChains+=gcss.length;
		totalPChains+=pcss.length;
		for(Chain c:gcss){
			totalGMentions+=c.spans.size();
			gChainSizeHisto.adjustOrPutValue(c.spans.size(), 1, 1);
		}
		for(Chain c:pcss){
			totalPMentions+=c.spans.size();
			pChainSizeHisto.adjustOrPutValue(c.spans.size(), 1, 1);			
		}
		CorefSolution goldSolution=new CorefSolution(gcss);
		int pi=0;
		int gi=0;
		int correct=0;
		int cSubSet=0;
		int cSupSet=0;
		while(pi<pcss.length && gi<gcss.length){
			//if two chains start on the same mention, then it might be interesting. Otherwise advance the pointer for the smaller chain
			boolean isCorrect=false;
			boolean sameStart=sameStart(gcss[gi],pcss[pi]);
			if(sameStart && sameChain(gcss[gi],pcss[pi])){
				corrChainSizeHisto.adjustOrPutValue(gcss[gi].spans.size(), 1, 1);
				isCorrect=true;
			}
			if(isCorrect){
				correct++;
			} else {
				if(isChainSubSet(pcss[pi],goldSolution,gold)){
					cSubSet++;
					subSetCorrChainSizeHisto.adjustOrPutValue(pcss[pi].spans.size(), 1, 1);
				}else if(isChainSuperSet(pcss[pi],goldSolution,gold)){
					cSupSet++;
					superSetCorrChainSizeHisto.adjustOrPutValue(pcss[pi].spans.size(), 1, 1);
				}
			}
			//Advance pointers
			if(sameStart){
				gi++;
				pi++;
			} else {
				int q=CHAIN_FIRST_MENTION_COMPARATOR.compare(pcss[pi], gcss[gi]);
				if(q<0)
					pi++;
				else
					gi++;				
			}
		}
		totalCorrChains+=correct;
		totalSubSetCorrChains+=cSubSet;
		totalSuperSetCorrChains+=cSupSet;
	}
	
	
	
	private boolean isChainSuperSet(Chain pred, CorefSolution goldSolution, Document gold) {
		Integer goldChainId=null;
		int i=0;
		while(goldChainId==null && i<pred.spans.size()){
			Span pSp=pred.spans.get(i);
			Span gSp=gold.getSpanFromUniqueKey(pSp.getUniqueIntKey());
			goldChainId=goldSolution.getSpanChainID(gSp);
			++i;
		}
		if(goldChainId==null)
			return false;
		else {
			int inChainCount=1;
			while(i<pred.spans.size()){
				Span pSp=pred.spans.get(i);
				Span gSp=gold.getSpanFromUniqueKey(pSp.getUniqueIntKey());
				Integer a=goldSolution.getSpanChainID(gSp);
				if(a!=null && !a.equals(goldChainId))
					return false;
				inChainCount++;
				++i;
			}
			if(inChainCount>2)
				return true;
		}		
		return false;
	}

	private boolean isChainSubSet(Chain pred, CorefSolution goldSolution, Document gold) {
		Integer goldChainId=goldSolution.getSpanChainID(gold.getSpanFromUniqueKey(pred.spans.get(0).getUniqueIntKey()));
		if(goldChainId==null)
			return false;
		for(int i=1;i<pred.spans.size();++i){
			Integer a=goldSolution.getSpanChainID(gold.getSpanFromUniqueKey(pred.spans.get(i).getUniqueIntKey()));
			if(a==null || !a.equals(goldChainId))
				return false;
		}
		return true;
	}

	private boolean sameChain(Chain c1, Chain c2) {
		if(c1.spans.size()!=c2.spans.size())
			return false;
		for(int i=0,m=c1.spans.size();i<m;++i)
			if(!sameSpan(c1.spans.get(i),c2.spans.get(i)))
				return false;
		return true;
	}

	private boolean sameStart(Chain c1, Chain c2) {
		return sameSpan(c1.spans.get(0),c2.spans.get(0));
	}

	private boolean sameSpan(Span s1, Span s2) {
		return s1.s.sentenceIndex==s2.s.sentenceIndex && s1.start==s2.start && s1.end==s2.end;
	}

	private boolean sameDoc(Document gold, Document pred) {
		if(!gold.header.equals(pred.header))
			return false;
		if(gold.sen.size()!=pred.sen.size())
			return false;
		for(int i=0,m=gold.sen.size();i<m;++i){
			Sentence g=gold.sen.get(i);
			Sentence p=pred.sen.get(i);
			if(g.forms.length!=p.forms.length)
				return false;
			for(int j=1,l=g.forms.length;j<l;++j)
				if(!g.forms[j].equals(p.forms[j]))
					return false;
		}
		return true;
	}


	public static Comparator<Chain> CHAIN_FIRST_MENTION_COMPARATOR=new Comparator<Chain>(){
		@Override
		public int compare(Chain arg0, Chain arg1) {
			Span c0s0=arg0.spans.get(0);
			Span c1s0=arg1.spans.get(0);
			if(c0s0.s.sentenceIndex<c1s0.s.sentenceIndex)
				return -1;
			if(c0s0.s.sentenceIndex>c1s0.s.sentenceIndex)
				return 1;
			
			if(c0s0.start<c1s0.start)
				return -1;
			if(c0s0.start>c1s0.start)
				return 1;
			
			if(c0s0.end>c1s0.end)
				return -1;
			if(c0s0.end<c1s0.end)
				return 1;
			
			
			throw new Error("two chains start with the same span !!!");
		}
	};
}
