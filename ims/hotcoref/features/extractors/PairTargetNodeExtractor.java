package ims.hotcoref.features.extractors;

import ims.hotcoref.decoder.HOTState;

import java.io.Serializable;

public class PairTargetNodeExtractor implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public final PairTargetNode ts;
	
	public PairTargetNodeExtractor(PairTargetNode ts){
		this.ts=ts;
	}
		
	
	public int getNodeInstIdx(int head,int dep,HOTState hotState){
		return getNodeInstIdx(head,dep,hotState,ts);
	}
	
	public int getNodeInstIdxPrecompArray(int[] a){
		return a[ts.ordinal()];
	}
	
	public static int getNodeInstIdxPrecompArray(int[] a,PairTargetNode ts){
		return a[ts.ordinal()];
	}
	
//	public abstract int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst);
	public static int getNodeInstIdx(int head,int dep,HOTState hotState,PairTargetNode ts){
		switch(ts){
		case MTo:	return dep;
		case MFrom:	return head;
		case MGP:	return hotState.heads[head];
		case MSib:	return head==0?-1:hotState.rmc[head];
//		case MGP:	return hotState.getHead(head);
//		case MSib:	return hotState.getRightMostChild(head);
		
		
		case MLinFirst: return hotState.chainBack[hotState.mention2chainIdx[head]];
		case MLinPre:	return hotState.chainFront[hotState.mention2chainIdx[head]];
		case MLinPre2:  return getLinearPreceding(2,hotState,head);
		case MLinPre3:	return getLinearPreceding(3,hotState,head);
//		case MTPreSS1:  return dep-1<1?-1:(((MNode) inst.nodes[dep-1]).span.s==((MNode) inst.nodes[dep]).span.s?dep-1:-1);
//		case MTPreSS2:  return dep-1<1?-1:(((MNode) inst.nodes[dep-2]).span.s==((MNode) inst.nodes[dep]).span.s?dep-2:-1);
//		case MTPreSS3:  return dep-1<1?-1:(((MNode) inst.nodes[dep-3]).span.s==((MNode) inst.nodes[dep]).span.s?dep-3:-1);
		default: throw new Error("!");
		}
	}

	private static int getLinearPreceding(int dist, HOTState hotState, int head) {
		int r=hotState.chainFront[hotState.mention2chainIdx[head]];
		for(--dist;dist>0 && r!=-1;--dist)
			r=hotState.leftPredecessor[r];
		return r;
	}

	public boolean firstOrder(){
		return ts==PairTargetNode.MFrom || ts==PairTargetNode.MTo;
	}
	
	public static PairTargetNodeExtractor getExtractor(PairTargetNode ts){
		return new PairTargetNodeExtractor(ts);
	}
	
	public static int[] getFOIdx(int head,int dep){
		return new int[]{head,dep};
	}
	
	private static final PairTargetNode[] PTE_TARGETS=PairTargetNode.values(); 
	public static int[] getAllIdx(int head,int dep,HOTState hotState){
		int[] r=new int[PTE_TARGETS.length];
		for(int i=0;i<PTE_TARGETS.length;++i)
			r[i]=getNodeInstIdx(head,dep,hotState,PTE_TARGETS[i]);
		return r;
	}
	
//	public static PairTargetNodeExtractor getExtractor(PairTargetNode ts){
//		switch(ts){
//		case MTo:		return new MToTNE(ts);
//		case MFrom:		return new MFromTNE(ts);
//		case MGP:		return new MGPTNE(ts);
//		case MSib:		return new MSibTNE(ts);
//			
//		case MTPre1:	return new MPre1TNE(ts);
//		case MTPre2:	return new MPre2TNE(ts);
//		case MTPre3:	return new MPre3TNE(ts);
//		case MTPreSS1:	return new MPreSS1TNE(ts);
//		case MTPreSS2:	return new MPreSS1TNE(ts);
//		case MTPreSS3:	return new MPreSS1TNE(ts);
//		}
//		throw new Error("!");
//	}
//	static final class MToTNE extends PairTargetNodeExtractor {
//		private static final long serialVersionUID =1L;
//		public MToTNE(PairTargetNode ts) {
//			super(ts);
//		}
//		public int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst){
//			return dep;
//		}
//	}
//	static final class MFromTNE extends PairTargetNodeExtractor {
//		private static final long serialVersionUID =1L;
//		public MFromTNE(PairTargetNode ts) {super(ts);}
//		public int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst){
//			return head;
//		}
//	}
//	static final class MGPTNE extends PairTargetNodeExtractor {
//		private static final long serialVersionUID =1L;
//		public MGPTNE(PairTargetNode ts) {super(ts);}
//		public int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst){
//			return hotState.getHead(head);
//		}
//	}
//	static final class MSibTNE extends PairTargetNodeExtractor {
//		private static final long serialVersionUID =1L;
//		public MSibTNE(PairTargetNode ts) {super(ts);}
//		public int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst){
//			return hotState.getRightMostChild(head);
//		}
//	}
//	static final class MPre1TNE extends PairTargetNodeExtractor {
//		private static final long serialVersionUID =1L;
//		public MPre1TNE(PairTargetNode ts) {super(ts);}
//		public int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst){
//			return dep-1;
//		}
//	}
//	static final class MPre2TNE extends PairTargetNodeExtractor {
//		private static final long serialVersionUID =1L;
//		public MPre2TNE(PairTargetNode ts) {super(ts);}
//		public int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst){
//			return dep-2;
//		}
//	}
//	static final class MPre3TNE extends PairTargetNodeExtractor {
//		private static final long serialVersionUID =1L;
//		public MPre3TNE(PairTargetNode ts) {super(ts);}
//		public int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst){
//			return dep-3;
//		}
//	}
//	static final class MPreSS1TNE extends PairTargetNodeExtractor {
//		private static final long serialVersionUID =1L;
//		public MPreSS1TNE(PairTargetNode ts) {super(ts);}
//		public int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst){
//			return dep-1<1?-1:(((MNode) inst.nodes[dep-1]).span.s==((MNode) inst.nodes[dep]).span.s?dep-1:-1);
//		}
//	}
//	static final class MPreSS2TNE extends PairTargetNodeExtractor {
//		private static final long serialVersionUID =1L;
//		public MPreSS2TNE(PairTargetNode ts) {super(ts);}
//		public int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst){
//			return dep-1<1?-1:(((MNode) inst.nodes[dep-2]).span.s==((MNode) inst.nodes[dep]).span.s?dep-2:-1);
//		}
//	}
//	static final class MPreSS3TNE extends PairTargetNodeExtractor {
//		private static final long serialVersionUID =1L;
//		public MPreSS3TNE(PairTargetNode ts) {super(ts);}
//		public int getNodeInstIdx(int head,int dep,HOTState hotState,Instance inst){
//			return dep-1<1?-1:(((MNode) inst.nodes[dep-3]).span.s==((MNode) inst.nodes[dep]).span.s?dep-3:-1);
//		}
//	}
	
	public String toString(){
		return ts.toString();
	}
}
