package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class T_TargetNETarget extends AbstractSinglePairFeature {
	private static final long serialVersionUID = -9121949277688949988L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;
	
	protected T_TargetNETarget(PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+"NE"+t2.ts.toString());
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {}

	@Override
	public int getBits(SymbolTable symTab) {
		return 0; //Uses no space
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {}

	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}

	@Override
	long appendBits(long initVal, Instance instance, SymbolTable symTab,int[] tnes, HOTState hotState) {
		int p=tse1.getNodeInstIdxPrecompArray(tnes);
		int q=tse2.getNodeInstIdxPrecompArray(tnes);
		if(p==q && q>=0)
			return -1;
		else
			return initVal;
	}

}
