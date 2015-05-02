package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.TrueFalse;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.ThreadPoolSingleton;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_TokenType extends AbstractPairEnumFeature<TrueFalse>{
	private static final long serialVersionUID = 5319509519797357764L;

	private final PairTargetNodeExtractor nodeExtractor;

	protected F_TokenType(PairTargetNodeExtractor nodeExtractor) {
		super(nodeExtractor.ts.toString()+"TokenType", TrueFalse.values());
		this.nodeExtractor=nodeExtractor;
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.addedByTokenType!=null)
			return;
		Callable<Void> c=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				inst.addedByTokenType=new byte[inst.nodes.length];
				for(int i=0;i<inst.nodes.length;++i)
					inst.addedByTokenType[i]=byTokenType(inst.nodes[i]);
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(c));
	}

	@Override
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		return inst.addedByTokenType[nodeExtractor.getNodeInstIdxPrecompArray(tnes)];
	}

	private static byte byTokenType(INode iNode) {
		final TrueFalse t;
		if(iNode.isVirtualNode())
			t=TrueFalse.VNode;
		else {
			MNode mn=(MNode) iNode;
			if(mn.span.addedByTokenType)
				t=TrueFalse.True;
			else
				t=TrueFalse.False;
		}
		return (byte) t.ordinal();
	}

	@Override
	public boolean firstOrderFeature() {
		return nodeExtractor.firstOrder();
	}
}
