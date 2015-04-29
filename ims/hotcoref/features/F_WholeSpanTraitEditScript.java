package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
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
import ims.util.EditDistance;
import ims.util.ThreadPoolSingleton;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_WholeSpanTraitEditScript extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = 2033889012356891883L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;

	
	private final TokenTraitExtractor tte;	
	private final int _tIdx;
	
	
	protected F_WholeSpanTraitEditScript(TokenTraitExtractor tte,PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"WholeSpan"+tte.tt.toString()+"EditScript");
		this.tte=tte;
		this._tIdx=tte.tt.ordinal();
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return tte.getBitsES(symTab);
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(tte.tt.getESType());		
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst, final SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.esTrait==null)
			inst.esTrait=new int[TokenTrait.values().length][][];
		if(inst.esTrait[_tIdx]!=null)
			return;
		
		inst.esTrait[_tIdx]=new int[inst.nodes.length][];
		
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.esTrait[_tIdx][nT]=new int[nT];
					for(int nF=0;nF<nT;++nF)
						inst.esTrait[_tIdx][nT][nF]=getES(nF,nT,inst,symTab);
				}
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
//		l.add(j);
	}

	private int getES(int nF,int nT, Instance inst, SymbolTable symTab) {
		INode fr=inst.nodes[nF];
		INode to=inst.nodes[nT];
		if(fr.isVirtualNode()) // || to.isVirtualNode())
			return AbstractSymbolMapping.getVNodeIntVal((VNode) fr);
		if(to.isVirtualNode())
			return AbstractSymbolMapping.getVNodeIntVal((VNode) to);
		MNode f=(MNode) fr;
		MNode t=(MNode) to;
		String c=getES(f.span,t.span);
		int i=symTab.esTrait[_tIdx].lookup(c);
		return i;
	}

	private String getES(Span sp1, Span sp2) {
		String s1=F_WholeSpanTraitEditDistance.getS(sp1, tte);
		String s2=F_WholeSpanTraitEditDistance.getS(sp2, tte);
		String c=EditDistance.editScript(s1, s2);
		return c;
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int r=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int l=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int r=tse2.getNodeInstIdxPrecompArray(tnes);
		int l=tse1.getNodeInstIdxPrecompArray(tnes);
		return inst.esTrait[_tIdx][r][l];
	}

	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
