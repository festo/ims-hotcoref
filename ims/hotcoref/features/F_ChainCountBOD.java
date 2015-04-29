package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.IBuckets;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;

public class F_ChainCountBOD <T extends Enum<T> & IBuckets<T>> extends AbstractPairEnumFeature<T> {
	private static final long serialVersionUID = -9044895370192639769L;

	private final PairTargetNodeExtractor tse;
	
	protected F_ChainCountBOD(T[] ts,PairTargetNodeExtractor tse) {
		super(tse.ts.toString()+"ChainCountBOD"+ts[0].getClass().getSimpleName(), ts);
		this.tse=tse;
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
//		int n=tse.getNodeInstIdx(headIdx, depIdx, hotState);
		int n=tse.getNodeInstIdxPrecompArray(tnes);
		return _ts[0].getBucket(hotState.getChainCount(n)).ordinal();
	}

}
