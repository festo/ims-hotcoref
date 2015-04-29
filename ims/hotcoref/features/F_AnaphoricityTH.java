package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.ProbabilityBuckets;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.ThreadPoolSingleton;

public class F_AnaphoricityTH extends AbstractPairEnumFeature<ProbabilityBuckets>{
	private static final long serialVersionUID = -6247899542245215241L;

	private final PairTargetNodeExtractor nodeExtractor;
	
	protected F_AnaphoricityTH(PairTargetNodeExtractor nodeExtractor) {
		super(nodeExtractor.ts.toString()+"Anaphoricity", ProbabilityBuckets.values());
		this.nodeExtractor=nodeExtractor;
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.anaphoricityBucketed!=null)
			return;
		inst.anaphoricityBucketed=new byte[inst.nodes.length];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0;i<inst.nodes.length;++i)
					inst.anaphoricityBucketed[i]=getByte(inst.nodes[i]);
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	private byte getByte(INode iNode) {
		final ProbabilityBuckets bu;
		if(iNode.isVirtualNode())
			bu=ProbabilityBuckets.getVNodeBucket();
		else {
			MNode m=(MNode) iNode;
			bu=ProbabilityBuckets.getBucket(m.span.anaphoricityPr);
		}
		return (byte) bu.ordinal();
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
//		int nodeIdx=nodeExtractor.getNodeInstIdx(headIdx,depIdx,hotState);
		int nodeIdx=nodeExtractor.getNodeInstIdxPrecompArray(tnes);
		return inst.anaphoricityBucketed[nodeIdx];
	}

	@Override
	public boolean firstOrderFeature() {
		return nodeExtractor.firstOrder();
	}
}
