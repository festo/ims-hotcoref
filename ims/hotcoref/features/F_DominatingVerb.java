package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.decoder.HOTState;
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

public class F_DominatingVerb extends AbstractAtomicPairFeature{
	private static final long serialVersionUID = -3836709208679519303L;

	private final PairTargetNodeExtractor tse;
	
	protected F_DominatingVerb(PairTargetNodeExtractor tse) {
		super(tse.ts.toString()+"DominatingVerb");
		this.tse=tse;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.forms.getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.Form);
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.dominatingVerb!=null)
			return;
		inst.dominatingVerb=new int[inst.nodes.length];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0;i<inst.nodes.length;++i)
					inst.dominatingVerb[i]=get(inst.nodes[i],symTab);
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	private int get(INode iNode, SymbolTable symTab) {
		if(iNode.isVirtualNode())
			return AbstractSymbolMapping.getVNodeIntVal((VNode) iNode);
		MNode mn=(MNode) iNode;
		String domVerb=getDominatingVerb(mn.span);
		if(domVerb==null)
			return AbstractSymbolMapping.NONE_IDX;
		else
			return symTab.forms.lookup(domVerb);
	}

	private String getDominatingVerb(Span span) {
		CFGNode n;
		if(span.cfgNode==null)
			n=span.s.ct.getMinimalIncludingNode(span.start, span.end);
		else
			n=span.cfgNode;
		int verbIndex=-1;
		do {
			int head=n.getHead();
			if(span.s.tags[head].startsWith("V")){
				verbIndex=head;
				break;
			}
		} while((n=n.getParent())!=null);
		if(verbIndex==-1)
			return null;
		else
			return span.s.forms[verbIndex];
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
//		return inst.dominatingVerb[tse.getNodeInstIdx(headIdx,depIdx,hotState)];
		return inst.dominatingVerb[tse.getNodeInstIdxPrecompArray(tnes)];
	}

	@Override
	public boolean firstOrderFeature() {
		return tse.firstOrder();
	}

}
