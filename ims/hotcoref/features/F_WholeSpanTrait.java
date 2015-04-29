package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.features.extractors.TokenTrait;
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

public class F_WholeSpanTrait extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = 3388924791521033730L;

	private final PairTargetNodeExtractor tse;
	private final TokenTraitExtractor tte;
	
	private final int ord;
	
	protected F_WholeSpanTrait(PairTargetNodeExtractor tse,TokenTraitExtractor tte) {
		super(tse.ts.toString()+"WholeSpan"+tte.tt.toString());
		this.tse=tse;
		this.tte=tte;
		this.ord=tte.tt.ordinal();
	}

	
	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(tte.tt.getType());
		t.add(tte.tt.getWSType());
	}
	
	@Override
	public int getBits(SymbolTable symTab) {
		return tte.getBitsWS(symTab);
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int nIdx=tse.getNodeInstIdx(headIdx,depIdx,hotState);
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int nIdx=tse.getNodeInstIdxPrecompArray(tnes);
		if(nIdx<0)
			return AbstractSymbolMapping.NONE_IDX;
		return inst.wsTokenTraits[ord][nIdx];
	}
	
	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.wsTokenTraits==null)
			inst.wsTokenTraits=new int[TokenTrait.values().length][];
		if(inst.wsTokenTraits[ord]!=null)
			return;
		inst.wsTokenTraits[ord]=new int[inst.nodes.length];
		Callable<Void> j=new Callable<Void>(){

			@Override
			public Void call() throws Exception {
				synchronized(tte.getWSMapping(symTab)){
					for(int i=0;i<inst.nodes.length;++i)
						inst.wsTokenTraits[ord][i]=computeIntVal(inst.nodes[i],symTab);
				}
				return null;
			}
			
		};
		l.add(j);
	}
	
	private int computeIntVal(INode node,SymbolTable symTab){
		if(node instanceof VNode)
			return AbstractSymbolMapping.getVNodeIntVal((VNode) node);
		MNode mNode=(MNode) node;
		int r=tte.lookupWS(symTab,mNode.span);
		return r;
	}

	@Override
	public boolean firstOrderFeature() {
		return tse.firstOrder();
	}	

}
