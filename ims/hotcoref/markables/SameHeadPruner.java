package ims.hotcoref.markables;

import ims.hotcoref.Options;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.util.IntPair;

import java.util.Map;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;

public class SameHeadPruner  extends AbstractMarkableExtractor{
	private static final long serialVersionUID = -7535482901581690080L;

//	@Override
//	public void extractMarkables(Sentence s, Set<Span> sink, String docName) {
//		TreeMap<IntPair,Span> tm=new TreeMap<IntPair,Span>(IntPair.IP_CMP_EMBEDDING);
//		tm.putAll(s.getSpanMap());
//		IntPair[] ips=new IntPair[tm.size()];
//		Span[] spans=new Span[tm.size()];
//		int q=0;
//		for(Entry<IntPair,Span> e:tm.entrySet()){
//			ips[q]=e.getKey();
//			spans[q++]=e.getValue();
//		}
//		if(Options.DEBUG){
//			for(int i=0;i<ips.length-1;++i){
//				int r=IP_LARGEST_EMBEDDING_FIRST_CMP.compare(ips[i], ips[i+1]);
//				if(r==2 || r==-2){
//					System.err.println(spans[i].s.d.docName);
//					System.err.println(spans[i]);
//					System.err.println(spans[i+1]);
//				}
//			}
//		}
//		for(int i=0;i<ips.length-1;++i){
//			for(int j=i+1;j<ips.length && spans[i].embeds(spans[j]);++j,++i)
//				if(spans[i].hd==spans[j].hd)
//					sink.remove(spans[j]);
//		}
//	}
	
	

	
	public static Comparator<IntPair> IP_LARGEST_EMBEDDING_FIRST_CMP=new Comparator<IntPair>(){
		@Override
		public int compare(IntPair arg0, IntPair arg1) {
			if(arg0.i1==arg1.i1 && arg0.i2==arg1.i2 && arg0!=arg1)
					throw new Error("!");
			//easy cases
			if(arg0.i2<arg1.i1) //first ends before next begins
				return -1;
			if(arg1.i2<arg0.i1) //next ends before first begins
				return 1;
			//difficult cases, they are somehow interleaved.
			//Start by making sure they are not interleaved in stupid ways
			if((arg0.i1>arg1.i1 && arg0.i2>arg1.i2) || (arg0.i1<arg1.i1 && arg0.i2<arg1.i2)){
				if(Options.DEBUG && false){ //sick of this, added && false
					System.err.println("Interleaved mentions extracted: ");
					System.err.println(arg0);
					System.err.println(arg1);
				}
				if(arg0.i1<arg1.i1)
					return -2;
				else
					return 2;
			}
			if(arg0.i1<arg1.i1) //arg0 begins before (and has to end after)
				return -1;
			if(arg1.i1<arg0.i1) //arg1 begins before (and has to end after
				return 1;
			if(arg0.i1==arg1.i1) //they begin on the same
				if(arg0.i2>arg1.i2)
					return -1;
				else
					return 1;
			throw new Error("!");
		}
	};

	public String toString(){
		return "SameHeadPruner";
	}
	
	public static void main(String[] args){
		IntPair[] ip=new IntPair[]{
				new IntPair(0,1),
				new IntPair(2,3),
				new IntPair(15,16),
				new IntPair(8,14),
				new IntPair(8,10),
				new IntPair(12,14),
				new IntPair(8,8),
				new IntPair(8,9),
				new IntPair(9,9),
				new IntPair(9,10)
		};
		int j=0;
		for(IntPair i:ip)
			System.out.println((++j)+"\t"+i.toString());
		System.out.println();
		System.out.println();
		Arrays.sort(ip,IP_LARGEST_EMBEDDING_FIRST_CMP);
		j=0;
		for(IntPair i:ip)
			System.out.println((++j)+"\t"+i.toString());
	}

	@Override
	public void extractMarkables(Sentence s, Set<Span> sink, String docName) {
		Map<IntPair,Span> m=new TreeMap<IntPair,Span>(IP_LARGEST_EMBEDDING_FIRST_CMP);
		m.putAll(s.getSpanMap());
		Span[] byHead=new Span[s.forms.length];
		for(Span sp:m.values())
			if(byHead[sp.hd]==null || (byHead[sp.hd]!=null && sp.size()-byHead[sp.hd].size()>0))
				byHead[sp.hd]=sp;
		for(Span sp:m.values())
			if(byHead[sp.hd]!=sp)
				sink.remove(sp);
	}
}
