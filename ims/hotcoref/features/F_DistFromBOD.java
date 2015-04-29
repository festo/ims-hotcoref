package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.IBuckets;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.symbols.SymbolTable;

public abstract class F_DistFromBOD <T extends Enum<T> & IBuckets<T>> extends AbstractPairEnumFeature<T> {
	private static final long serialVersionUID = -3797551176302048924L;

	final PairTargetNodeExtractor tse;
	
	protected F_DistFromBOD(String name, T[] ts,PairTargetNodeExtractor tse) {
		super(name, ts);
		this.tse=tse;
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		//do nothing
	}

	@Override
	public boolean firstOrderFeature() {
		return tse.firstOrder();
	}

	static class F_SentenceDistFromBOD<T extends Enum<T> & IBuckets<T>> extends F_DistFromBOD<T> {
		private static final long serialVersionUID = 5486551192170912540L;
		protected F_SentenceDistFromBOD(T[] ts,PairTargetNodeExtractor tse) {
			super(tse.ts.toString()+"SentenceDistBOD"+ts[0].getClass().getSimpleName(), ts, tse);
		}
		@Override
//		int getIntValue(Instance inst, int headIdx, int depIdx,HOTState hotState) {
		int getIntValue(Instance inst, int[] tnes,HOTState hotState) {
//			int i=tse.getNodeInstIdx(headIdx, depIdx, hotState);
			int i=tse.getNodeInstIdxPrecompArray(tnes);
			if(i==0)
				return _ts[0].getVNodeBucket().ordinal();
			int s=((MNode) inst.nodes[i]).sIdx;
			return _ts[0].getBucket(s).ordinal();
		}
	}
	
	static class F_MentionDistFromBOD<T extends Enum<T> & IBuckets<T>> extends F_DistFromBOD<T> {
		private static final long serialVersionUID = 1L;
		protected F_MentionDistFromBOD(T[] ts,PairTargetNodeExtractor tse) {
			super(tse.ts.toString()+"MentionDistBOD"+ts[0].getClass().getSimpleName(), ts, tse);
		}
		@Override
//		int getIntValue(Instance inst, int headIdx, int depIdx,	HOTState hotState) {
		int getIntValue(Instance inst, int[] tnes,HOTState hotState) {
//			int i=tse.getNodeInstIdx(headIdx, depIdx, hotState);
			int i=tse.getNodeInstIdxPrecompArray(tnes);
			if(i==0)
				return _ts[0].getVNodeBucket().ordinal();
			return _ts[0].getBucket(i-1).ordinal();
		}
	}
	
	public static <T extends Enum<T> & IBuckets<T>> F_DistFromBOD<T> getFeature(T[] values,String type,PairTargetNodeExtractor tse){
		if(type.equals("Sentence"))
			return new F_SentenceDistFromBOD<T>(values,tse);
		else if(type.equals("Mention"))
			return new F_MentionDistFromBOD<T>(values,tse);
		throw new Error("!!");
	}
}
