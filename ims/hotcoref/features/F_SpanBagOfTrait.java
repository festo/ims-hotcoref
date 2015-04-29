package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.features.extractors.TokenTraitExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.symbols.AbstractSymbolMapping;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_SpanBagOfTrait extends AbstractMultiPairFeature {
	private static final long serialVersionUID = -4023431365077040222L;

	private final PairTargetNodeExtractor tse;
	private final TokenTraitExtractor tte;
	
	protected F_SpanBagOfTrait(PairTargetNodeExtractor tse,TokenTraitExtractor tte) {
		super(tse.ts.toString()+"BagOf"+tte.tt.toString());
		this.tse=tse;
		this.tte=tte;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return tte.getBits(symTab);
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(tte.tt.getType());
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst, final SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
//		if(inst.bagOfTraits==null)
//			inst.bagOfTraits=new int[PairTargetNode.values().length][TokenTrait.values().length][][];
//		if(inst.bagOfTraits[_sIdx][_tIdx]!=null)
//			return;
//		inst.bagOfTraits[_sIdx][_tIdx]=new int[inst.nodes.length][];
//		Callable<Void> q=new Callable<Void>(){
//			@Override
//			public Void call() throws Exception {
//				for(int i=0;i<inst.nodes.length;++i)
//					inst.bagOfTraits[_sIdx][_tIdx][i]=getBag(i,inst,symTab);
//				return null;
//			}
//		};
//		l.add(q);
	}

	private long[] getBag(int nodeIdx, Instance inst, SymbolTable symTab) {
		INode node=inst.nodes[nodeIdx];
		if(node.isVirtualNode())
			return new long[]{AbstractSymbolMapping.getVNodeIntVal((VNode) node)};
		MNode m=(MNode) node;
		Span sp=m.span;
		long[] q=new long[sp.size()];
		int[] arr=tte.getArr(sp.s);
		for(int r=sp.start,a=0;r<=sp.end;++r,++a)
			q[a]=arr[r];
		return q;
	}

	@Override
//	long[] getLongArr(SymbolTable symTab,Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int nodeIdx=tse.getNodeInstIdx(headIdx,depIdx,hotState);
	long[] getLongArr(SymbolTable symTab,Instance inst,int[] tnes,HOTState hotState) {
		int nodeIdx=tse.getNodeInstIdxPrecompArray(tnes);
		if(nodeIdx<0)
			return new long[]{AbstractSymbolMapping.NONE_IDX};
		return getBag(nodeIdx,inst,symTab);
////		return inst.bagOfTraits[_sIdx][_tIdx][nodeIdx];
//		long[] r=new long[inst.bagOfTraits[_sIdx][_tIdx][nodeIdx].length];
//		int p=0;
//		for(int i:inst.bagOfTraits[_sIdx][_tIdx][nodeIdx])
//			r[++p]=i;
//		return r;
	}

	@Override
	public boolean firstOrderFeature() {
		return tse.firstOrder();
	}
}
