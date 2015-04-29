package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.TrueFalse;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.ThreadPoolSingleton;

public class F_Nested extends AbstractPairEnumFeature<TrueFalse>{
	private static final long serialVersionUID = 3370762256629751887L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;
	
	protected F_Nested(PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"Nested", TrueFalse.values());
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
		if(areNested(f,t))
			return TrueFalse.True;
		else
			return TrueFalse.False;
	}

	public static boolean areNested(Span s1,Span s2){
		return s1.s==s2.s && ((s1.start<=s2.start && s1.end>=s2.end) || (s2.start<=s1.start && s2.end>=s1.end));
	}

	@Override
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int t=tse2.getNodeInstIdxPrecompArray(tnes);
		int s=tse1.getNodeInstIdxPrecompArray(tnes);
		int l=Math.min(s,t);
		if(l<0)
			return -1;
		int r=Math.max(s,t);
		return inst.nested[r][l];
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.nested!=null)
			return;
		inst.nested=new byte[inst.nodes.length][];
		
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.nested[nT]=new byte[nT];
					for(int nF=0;nF<nT;++nF)
						inst.nested[nT][nF]=(byte) computeT(nF,nT,inst).ordinal();
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
