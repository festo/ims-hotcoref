package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.Options;
import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.IBuckets;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.symbols.SymbolTable;

public class F_MentionSize <T extends Enum<T> & IBuckets<T>> extends AbstractPairEnumFeature<T> implements ISingleMentionFeature {
	private static final long serialVersionUID = -4896664871318242607L;

	private final PairTargetNodeExtractor tse;
	
	protected F_MentionSize(PairTargetNodeExtractor tse,T[] ts) {
		super(tse.ts.toString()+"MentionSize"+ts[0].getClass().getSimpleName(), ts);
		this.tse=tse;
		Options.dontClearSpans=true; //We could get rid of this dependency by introducing another array in Instances, would be faster during training too, but cba for the moment TODO
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
	}

	@Override
	public boolean firstOrderFeature() {
		return tse.firstOrder();
	}

	@Override
	public int getIntValue(Instance inst, int idx) {
		if(idx<=0)
			return _ts[0].getVNodeBucket().ordinal();
		MNode mNode=(MNode) inst.nodes[idx];
		int size=mNode.span.size();
		return _ts[0].getBucket(size).ordinal();
	}

	@Override
	int getIntValue(Instance inst, int[] tnes, HOTState hotState) {
		int idx=tse.getNodeInstIdxPrecompArray(tnes);
		return getIntValue(inst,idx);
	}

}
