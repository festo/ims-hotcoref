package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.Num;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.ThreadPoolSingleton;

public class F_Number extends AbstractPairEnumFeature<Num> {
	private static final long serialVersionUID = 4988768071632378832L;

	private final PairTargetNodeExtractor nodeExtractor;
	
	protected F_Number(PairTargetNodeExtractor nodeExtractor) {
		super(nodeExtractor.ts.toString()+"Number", Num.values());
		this.nodeExtractor=nodeExtractor;
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst, SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.number!=null)
			return;
		inst.number=new byte[inst.nodes.length];
		Callable<Void> job=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0;i<inst.nodes.length;++i)
					inst.number[i]=getByte(inst.nodes[i]);
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(job));
	}

	private byte getByte(INode iNode) {
		final Num n;
		if(iNode.isVirtualNode())
			n=Num.VNODE;
		else 
			n=((MNode) iNode).span.number;
		
		return (byte) n.ordinal();
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		return inst.number[nodeExtractor.getNodeInstIdx(headIdx,depIdx,hotState)];
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		return inst.number[nodeExtractor.getNodeInstIdxPrecompArray(tnes)];
	}
	
	@Override
	public boolean firstOrderFeature() {
		return nodeExtractor.firstOrder();
	}

}
