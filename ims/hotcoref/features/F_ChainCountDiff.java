package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.IBuckets;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;

public class F_ChainCountDiff <T extends Enum<T> & IBuckets<T>> extends AbstractPairEnumFeature<T> {
	private static final long serialVersionUID = 508311737362577893L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;
	
	protected F_ChainCountDiff(T[] ts,PairTargetNodeExtractor tse1,PairTargetNodeExtractor tse2) {
		super(tse1.ts.toString()+tse2.ts.toString()+"ChainCountDiff"+ts[0].getClass().getSimpleName(), ts);
		this.tse1=tse1;
		this.tse2=tse2;
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		//do nothing
	}

	@Override
	public boolean firstOrderFeature() {
		return false;
	}

	@Override
//	int getIntValue(Instance inst, int headIdx, int depIdx, HOTState hotState) {
	int getIntValue(Instance inst, int[] tnes, HOTState hotState) {
//		int n1=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
//		int n2=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
		int n1=tse1.getNodeInstIdxPrecompArray(tnes);
		int n2=tse2.getNodeInstIdxPrecompArray(tnes);
		if(n1<0 || n2<0)
			return _ts[0].getBucket(-1).ordinal();
		int dist=hotState.getChainCount(n1)-hotState.getChainCount(n2);
		return _ts[0].getBucket(Math.abs(dist)).ordinal();
	}

}
