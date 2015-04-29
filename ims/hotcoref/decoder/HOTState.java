package ims.hotcoref.decoder;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HOTState {

	public final int[] heads;
	public final int[] rmc;
	public final int[] leftPredecessor;
	public final int[] chainFront;
	public final int[] chainBack;
	public final int[] chainSize;
	
	
	public final int[] mention2chainIdx;
	
	private final int[] chainCount;
	private int chCount=0;
	public int realChCount=0;
	float score=0;
	private final List<HOTEdge> hotEdges;
	private boolean correct=true;
	int edgeLenSum=0;
	
	public HOTState(int len){
		heads=new int[len];
		rmc=new int[len];
		leftPredecessor=new int[len];
		mention2chainIdx=new int[len];
		chainCount=new int[len];
		chainFront=new int[len];
		chainBack=new int[len];
		chainSize=new int[len];
		hotEdges=new ArrayList<HOTEdge>();
		Arrays.fill(heads, -1);
		Arrays.fill(rmc, -1);
		Arrays.fill(chainCount, -1);
		Arrays.fill(leftPredecessor, -1);
		Arrays.fill(chainFront,-1);
		Arrays.fill(chainBack,-1);
	}
	
	private HOTState(HOTState s){
		this(s,s.heads.length);
	}
	
	public HOTState(HOTState s, int size) {
		//Copy constructor -- need to handle rmc array separarately !
		final int len=s.heads.length;
		this.heads=new int[len];
		this.rmc=new int[len];
		this.leftPredecessor=new int[len];
		this.chainFront=new int[len];
		this.chainBack=new int[len];
		this.mention2chainIdx=new int[len];
		this.chainCount=new int[len];
		this.chainSize=new int[len];
		this.chCount=s.chCount;
		this.score=s.score;
		System.arraycopy(s.heads, 0, heads, 0, size);
		System.arraycopy(s.leftPredecessor, 0, leftPredecessor, 0, size);
		System.arraycopy(s.chainCount, 0, chainCount, 0, size);
		System.arraycopy(s.mention2chainIdx, 0, mention2chainIdx, 0, size);
		System.arraycopy(s.chainFront, 0, chainFront, 0, chCount+1);
		System.arraycopy(s.chainBack,0,chainBack,0,chCount+1);
		System.arraycopy(s.chainSize, 0, chainSize, 0, chCount+1);
		hotEdges=new ArrayList<HOTEdge>(size<s.hotEdges.size()?s.hotEdges.subList(0, size):s.hotEdges);
		Arrays.fill(heads,size,len,-1);
		Arrays.fill(leftPredecessor, size,len,-1);
		Arrays.fill(chainCount, size,len,-1);
		Arrays.fill(chainFront,chCount+1,len,-1);
		Arrays.fill(chainBack,chCount+1,len,-1);
		Arrays.fill(chainSize, chCount+1,len,-1);
		this.correct=s.correct;
		this.edgeLenSum=s.edgeLenSum;
	}

	void appendEdge(HOTEdge he){
//		int dep=he.getDepIdx();
		int dep=he.e.depIdx;
		if(heads[dep]!=-1)
			throw new Error("!");
//		int head=he.getHeadIdx();
		int head=he.e.headIdx;
		if(head==0){
			++chCount;
			mention2chainIdx[dep]=chCount;
			chainFront[chCount]=dep;
			chainBack[chCount]=dep;
			chainSize[chCount]=1;
		} else {
			mention2chainIdx[dep]=mention2chainIdx[head];
			chainFront[mention2chainIdx[head]]=dep;
			if(rmc[head]==-1)
				realChCount++;
			chainSize[mention2chainIdx[head]]++;
		}
		heads[dep]=head;
		rmc[head]=dep;
		chainCount[dep]=chCount;
		score+=he.hotScore+he.e.getScore();
		hotEdges.add(he);
		edgeLenSum+=dep-head;
		leftPredecessor[dep]=getLeftPredecessor(dep,head);
	}
	
	private int getLeftPredecessor(int dep, int head) {
		if(head==0)
			return 0;
		int lp=head;
		for(int i=head+1;i<dep;++i)
			if(mention2chainIdx[head]==mention2chainIdx[i])
				lp=i;
		return lp;
	}

	public int getEdgeLenSum(){
		return edgeLenSum;
	}
	
	public int getHead(int idx){
		return heads[idx];
	}

	public int getRightMostChild(int head) {
		if(head==0)
			return -1;
		else
			return rmc[head];
	}
	
	public int getChainCount(int idx){
		if(idx<0)
			return -1;
		else if(chainCount[idx]<0)
			return chCount;
		else
			return chainCount[idx];
	}
	
	public HOTState copy(){
		HOTState n=new HOTState(this);
		System.arraycopy(rmc, 0, n.rmc, 0, rmc.length);
		return n;
	}

	public double score() {
		return score;
	}

	public List<HOTEdge> getHOTEdges(){
		return hotEdges;
	}
//	
//	private static final Comparator<HOTState> SCORE_COMPARATOR=new Comparator<HOTState>(){
//		@Override
//		public int compare(HOTState arg0, HOTState arg1) {
//			if(arg0.score<arg1.score)
//				return -1;
//			else if (arg0.score>arg1.score)
//				return 1;
//			else
//				return 0;
//		}
//	};
//	private static final Comparator<HOTState> REV_SCORE_COMPARATOR=Collections.reverseOrder(SCORE_COMPARATOR);

	public int[] getHeads() {
		return heads;
	}
	
	public boolean getCorrect(){
		return correct;
	}
	
	public void setCorrect(boolean c){
		this.correct=c;
	}
	
	public int getChainIdx(int nodeIdx){
		return mention2chainIdx[nodeIdx];
	}

	public HOTState copyUntil(int size) {
		HOTState n=new HOTState(this,size);
		Arrays.fill(n.rmc, -1);
		for(int i=1;i<size;++i)
			n.rmc[heads[i]]=i;
		return n;
	}
	
	public static final Comparator<HOTState> SCORE_AND_EDGELEN_COMPARATOR=new Comparator<HOTState>(){
		@Override
		public int compare(HOTState arg0, HOTState arg1) {
			if(arg0.score<arg1.score) //lower score means smaller (worse)
				return -1;
			else if (arg0.score>arg1.score)
				return 1;
			else if (arg0.edgeLenSum>arg1.edgeLenSum) //greater edge len sum means smaller (worse)
				return -1;
			else if (arg0.edgeLenSum<arg1.edgeLenSum)
				return 1;
			else
				return 0;
		}
	};
	
	public static final Comparator<HOTState> REV_SCORE_AND_EDGELEN_COMPARATOR=Collections.reverseOrder(SCORE_AND_EDGELEN_COMPARATOR);
}
