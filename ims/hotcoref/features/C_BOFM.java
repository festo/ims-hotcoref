package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNode;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class C_BOFM extends AbstractMultiPairFeature {
	private static final long serialVersionUID = -2812739200531977138L;

	private final ISingleMentionMultiFeature smf;
	
	protected C_BOFM(ISingleMentionMultiFeature smf) {
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
		int chainIdx=hotState.mention2chainIdx[head];
		int size=hotState.chainSize[chainIdx];
		long[][] rt=new long[size][];
		for(int q=0,i=hotState.chainFront[chainIdx];i!=0;++q,i=hotState.leftPredecessor[i])
			rt[q]=smf.getLongValues(inst, i);
		long[] r=merge(rt);
		return r;
	}

	private static long[] merge(long[][] rt) {
		int total=0;
		for(long[] p:rt)
			total+=p.length;
		long[] r=new long[total];
		int offset=0;
		for(long[] p:rt){
			System.arraycopy(p, 0, r, offset, p.length);
			offset+=p.length;
		}
		return r;
	}

}
