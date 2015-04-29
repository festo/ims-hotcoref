package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNode;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.AbstractSymbolMapping;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class C_BOF extends AbstractMultiPairFeature {
	private static final long serialVersionUID = 2114023081066276487L;
	
	private final ISingleMentionFeature smf;
	
	protected C_BOF(ISingleMentionFeature smf) {
		super("CBOF_"+smf.getName());
		this.smf=smf;
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		smf.addSymbolTypes(t);
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return smf.getBits(symTab);
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		smf.XfillFillInstanceJobs(inst, symTab, l, fjs);
	}

	@Override
	public boolean firstOrderFeature() {
		return false;
	}

	@Override
	long[] getLongArr(SymbolTable symTab, Instance inst, int[] tnes,HOTState hotState) {
		int head=PairTargetNodeExtractor.getNodeInstIdxPrecompArray(tnes, PairTargetNode.MFrom);
		if(head==0)
			return new long[]{AbstractSymbolMapping.NONE_IDX};
		int chainIdx=hotState.mention2chainIdx[head];
		int size=hotState.chainSize[chainIdx];
		long[] r=new long[size];
		for(int q=0,i=hotState.chainFront[chainIdx];i!=0;++q,i=hotState.leftPredecessor[i])
			r[q]=smf.getIntValue(inst, i);
		return r;
	}

}
