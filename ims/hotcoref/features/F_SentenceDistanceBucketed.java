package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.IBuckets;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.ThreadPoolSingleton;

public class F_SentenceDistanceBucketed <T extends Enum<T> & IBuckets<T>> extends AbstractPairEnumFeature<T> {

	private static final long serialVersionUID = 1897966954743173467L;

	private final int bIdx;
	
	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;
	
	protected F_SentenceDistanceBucketed(T[] buckets,PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"Distance"+buckets[0].getClass().getSimpleName(), buckets);
		this.bIdx=IBuckets.bIdxMap.indexOf(buckets[0].getClass());
		this.tse1=t1;
		this.tse2=t2;
	}

	protected T computeT(int nF,int nT,Instance inst){
		INode from=inst.nodes[nF];
		INode to=inst.nodes[nT];
		if(from instanceof VNode || to instanceof VNode)
			return _ts[0].getVNodeBucket();
		int sentIdFrom=((MNode)from).span.s.sentenceIndex;
		int sentIdTo=((MNode)to).span.s.sentenceIndex;
		return _ts[0].getBucket(Math.abs(sentIdFrom-sentIdTo));
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int t=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int s=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int t=tse2.getNodeInstIdxPrecompArray(tnes);
		int s=tse1.getNodeInstIdxPrecompArray(tnes);
		int l=Math.min(s, t);
		if(l<0)
			return _ts[0].getVNodeBucket().ordinal();
		int r=Math.max(s, t);
		return inst.sentDist[bIdx][r][l];
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.sentDist==null)
			inst.sentDist=new byte[IBuckets.bIdxMap.size()][][];
		if(inst.sentDist[bIdx]!=null)
			return;
		inst.sentDist[bIdx]=new byte[inst.nodes.length][];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.sentDist[bIdx][nT]=new byte[nT];
					for(int nF=0;nF<nT;++nF)
						inst.sentDist[bIdx][nT][nF]=(byte) computeT(nF,nT,inst).ordinal();
				}
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}