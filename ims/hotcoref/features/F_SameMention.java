package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.TrueFalse;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;

public class F_SameMention extends AbstractPairEnumFeature<TrueFalse>{
	private static final long serialVersionUID = 6866409164999721298L;

	private final PairTargetNodeExtractor tn1;
	private final PairTargetNodeExtractor tn2;

	
	protected F_SameMention(PairTargetNodeExtractor tn1,PairTargetNodeExtractor tn2) {
		super(tn1.ts.toString()+tn2.ts.toString()+"SameMention", TrueFalse.values());
		this.tn1=tn1;
		this.tn2=tn2;
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		//do nothing
	}

	@Override
	public boolean firstOrderFeature() {
		return tn1.firstOrder() && tn2.firstOrder();
	}

	@Override
//	int getIntValue(Instance inst, int headIdx, int depIdx, HOTState hotState) {
//		int p=tn1.getNodeInstIdx(headIdx, depIdx, hotState);
//		int q=tn2.getNodeInstIdx(headIdx, depIdx, hotState);
	int getIntValue(Instance inst, int[] tnes, HOTState hotState) {
		int p=tn1.getNodeInstIdxPrecompArray(tnes);
		int q=tn2.getNodeInstIdxPrecompArray(tnes);
		if(p==q){
			return TrueFalse.True.ordinal();
		} else {
			int s=Math.min(p, q);
			if(s==-1)
				return TrueFalse.None.ordinal();
			else if(s==0)
				return TrueFalse.VNode.ordinal();
			else
				return TrueFalse.False.ordinal();
		}
	}

}
