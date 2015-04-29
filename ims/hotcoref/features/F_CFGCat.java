package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.CFGTarget;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.symbols.AbstractSymbolMapping;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;
import ims.util.ThreadPoolSingleton;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_CFGCat extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = 9103390401645934882L;

	private final PairTargetNodeExtractor tse;
	private final CFGTarget cfgTarget;
	private final int ord;
	
	protected F_CFGCat(PairTargetNodeExtractor tse,CFGTarget cfgTarget) {
		super(tse.ts.toString()+"CFG"+cfgTarget.toString()+"Category");
		this.tse=tse;
		this.cfgTarget=cfgTarget;
		ord=cfgTarget.ordinal();
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.cats.getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.Category);
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
//		return inst.cfgNodeCat[ord][tse.getNodeInstIdx(headIdx,depIdx,hotState)];
		return inst.cfgNodeCat[ord][tse.getNodeInstIdxPrecompArray(tnes)];
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.cfgNodeCat==null)
			inst.cfgNodeCat=new int[CFGTarget.values().length][];
		if(inst.cfgNodeCat[ord]!=null)
			return;
		
		inst.cfgNodeCat[ord]=new int[inst.nodes.length];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0;i<inst.nodes.length;++i)
					inst.cfgNodeCat[ord][i]=computeValue(inst.nodes[i],symTab);
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
//		l.add(j);
	}

	private int computeValue(INode iNode, SymbolTable symTab) {
		if(iNode instanceof VNode)
			return AbstractSymbolMapping.getVNodeIntVal((VNode) iNode);
		MNode node=(MNode) iNode;
		if(node.span.cfgNode==null)
			return AbstractSymbolMapping.NONE_IDX;
		CFGNode n=node.span.cfgNode;
		CFGNode target=cfgTarget.getCFGNode(n);
		if(target==null)
			return AbstractSymbolMapping.NONE_IDX;
		else
			return symTab.cats.lookup(target.getLabel());
	}

	@Override
	public boolean firstOrderFeature() {
		return tse.firstOrder();
	}
}
