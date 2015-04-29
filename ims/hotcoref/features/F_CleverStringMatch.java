package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.TrueFalse;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.lang.Language;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.ThreadPoolSingleton;

public class F_CleverStringMatch extends AbstractPairEnumFeature<TrueFalse>{
	private static final long serialVersionUID = 3751308473405879457L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;
	
	protected F_CleverStringMatch(PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"CleverStringMatch",TrueFalse.values());
		this.tse1=t1;
		this.tse2=t2;
	}

	protected TrueFalse computeT(int nF,int nT,Instance inst) {
		INode from=inst.nodes[nF];
		INode to=inst.nodes[nT];
		if(from instanceof VNode || to instanceof VNode)
			return TrueFalse.VNode;
		Span f=((MNode) from).span;
		Span t=((MNode) to).span;
		if(Language.getLanguage().cleverStringMatch(f, t))
			return TrueFalse.True;
		else
			return TrueFalse.False;
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
//		int p=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int q=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
		int p=tse2.getNodeInstIdxPrecompArray(tnes);
		int q=tse1.getNodeInstIdxPrecompArray(tnes);
		int r=Math.max(p, q);
		int l=Math.min(p, q);
		if(l<0)
			return TrueFalse.None.ordinal();
		else
			return inst.cleverStringMatch[r][l];
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.cleverStringMatch!=null)
			return;
		inst.cleverStringMatch=new byte[inst.nodes.length][];
		
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.cleverStringMatch[nT]=new byte[nT];
					for(int nF=0;nF<nT;++nF)
						inst.cleverStringMatch[nT][nF]=(byte) computeT(nF,nT,inst).ordinal();
				}
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}	

	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
