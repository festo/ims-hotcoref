package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.TrueFalse;
import ims.hotcoref.features.extractors.PairTargetNode;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;

public class F_SameChain extends AbstractPairEnumFeature<TrueFalse>{
	private static final long serialVersionUID = -1621931168067833248L;

	private final PairTargetNodeExtractor tn1;
	private final PairTargetNodeExtractor tn2;
	
	protected F_SameChain(PairTargetNodeExtractor tn1,PairTargetNodeExtractor tn2) {
		super(tn1.ts.toString()+tn2.ts.toString()+"SameChain", TrueFalse.values());
		this.tn1=tn1;
		this.tn2=tn2;
		if(tn1.ts==PairTargetNode.MTo || tn2.ts==PairTargetNode.MTo)
			throw new Error("dont include MTo in SameChain feature, use MFrom");
		if((tn1.ts==PairTargetNode.MFrom && tn2.ts==PairTargetNode.MFrom))
			throw new Error("Makes no sense to check for same chain on "+tn1.toString() + " and "+tn2.toString());
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
//		int a=tn1.getNodeInstIdx(headIdx, depIdx, hotState);
//		int b=tn2.getNodeInstIdx(headIdx, depIdx, hotState);
	int getIntValue(Instance inst, int[] tnes, HOTState hotState) {
		int a=tn1.getNodeInstIdxPrecompArray(tnes);
		int b=tn2.getNodeInstIdxPrecompArray(tnes);
		if(a==0 || b==0)
			return TrueFalse.VNode.ordinal();
		if(a==-1 || b==-1)
			return TrueFalse.None.ordinal();
		if(hotState.getChainIdx(a) == hotState.getChainIdx(b))
			return TrueFalse.True.ordinal();
		else
			return TrueFalse.False.ordinal();
	}
	
}
