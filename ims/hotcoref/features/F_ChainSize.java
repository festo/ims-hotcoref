package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.IBuckets;
import ims.hotcoref.features.extractors.PairTargetNode;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;

public class F_ChainSize <T extends Enum<T> & IBuckets<T>> extends AbstractPairEnumFeature<T> {
	private static final long serialVersionUID = -530276206626079729L;

	protected F_ChainSize(T[] ts) {
		super("ChainSize"+ts[0].getClass().getSimpleName(), ts);
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {}

	@Override
	public boolean firstOrderFeature() {
		return false;
	}

	@Override
	int getIntValue(Instance inst, int[] tnes, HOTState hotState) {
		int head=PairTargetNodeExtractor.getNodeInstIdxPrecompArray(tnes, PairTargetNode.MFrom);
		if(head==0)
			return _ts[0].getVNodeBucket().ordinal();
		int cix=hotState.mention2chainIdx[head];
		int csize=hotState.chainSize[cix];
		return _ts[0].getBucket(csize).ordinal();
	}

}
