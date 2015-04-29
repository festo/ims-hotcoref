package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.IBuckets;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;

public class F_MentionDist <T extends Enum<T> & IBuckets<T>> extends AbstractPairEnumFeature<T> {
	private static final long serialVersionUID = 4155428422539922110L;
	
	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;
	
	protected F_MentionDist(T[] ts,PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"MentionDist"+ts[0].getClass().getSimpleName(), ts);
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs) {
		return; //Do nothing
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int r=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int l=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int r=tse2.getNodeInstIdxPrecompArray(tnes);
		int l=tse1.getNodeInstIdxPrecompArray(tnes);

		if(l<=0)
			return _ts[0].getVNodeBucket().ordinal();
		else
			return _ts[0].getBucket(Math.abs(l-r)).ordinal();
	}

	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
