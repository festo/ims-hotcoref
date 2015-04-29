package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.Gender;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.ThreadPoolSingleton;

public class F_Gender extends AbstractPairEnumFeature<Gender> implements ISingleMentionFeature {
	private static final long serialVersionUID = 7460524611469166156L;

	private final PairTargetNodeExtractor nodeExtractor;
	
	protected F_Gender(PairTargetNodeExtractor nodeExtractor) {
		super(nodeExtractor.ts.toString()+"Gender", Gender.values());
		this.nodeExtractor=nodeExtractor;
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.gender!=null)
			return;
		inst.gender=new byte[inst.nodes.length];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0,m=inst.nodes.length;i<m;++i)
					inst.gender[i]=(byte) inst.nodes[i].getGender().ordinal();
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	@Override
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int idx=nodeExtractor.getNodeInstIdxPrecompArray(tnes);
		return getIntValue(inst, idx);
	}

	public int getIntValue(Instance inst, int idx) {
		if(idx<0)
			return Gender.None.ordinal();
		else
			return inst.gender[idx];
	}

	@Override
	public boolean firstOrderFeature() {
		return nodeExtractor.firstOrder();
	}
}
