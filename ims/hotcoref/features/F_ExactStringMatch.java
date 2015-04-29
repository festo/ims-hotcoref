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
import ims.hotcoref.symbols.SymbolTable;
import ims.util.ThreadPoolSingleton;

public class F_ExactStringMatch extends AbstractPairEnumFeature<TrueFalse>{
	private static final long serialVersionUID = 5946176248718820774L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;
	
	protected F_ExactStringMatch(PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"ExactStringMatch", TrueFalse.values());
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.exactStringMatch!=null)
			return;

		inst.exactStringMatch=new byte[inst.nodes.length][];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.exactStringMatch[nT]=new byte[nT];
					for(int nF=0;nF<nT;++nF)
						inst.exactStringMatch[nT][nF]=exactStringMatch(nF,nT,inst);
				}
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	private byte exactStringMatch(int from,int to, Instance inst) {
		TrueFalse t=exactStringMatch(inst.nodes[from],inst.nodes[to]);
		return (byte) t.ordinal();
	}
	
	private TrueFalse exactStringMatch(INode from,INode to){
		if(from.isVirtualNode() || to.isVirtualNode())
			return TrueFalse.VNode;
		MNode f=(MNode) from;
		MNode t=(MNode) to;
		if(matches(f.span,t.span))
			return TrueFalse.True;
		else
			return TrueFalse.False;
	}

	private boolean matches(Span s1, Span s2) {
		if(s1.size()!=s2.size())
			return false;
		for(int i1=s1.start,i2=s2.start,m1=s1.end;i1<=m1;++i1,++i2)
			if(!s1.s.forms[i1].equals(s2.s.forms[i2]))
				return false;
		return true;
	}

	@Override
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int p=tse2.getNodeInstIdxPrecompArray(tnes);
		int q=tse1.getNodeInstIdxPrecompArray(tnes);

		int r=Math.max(p, q);
		int l=Math.min(p, q);
		if(l<0)
			return TrueFalse.None.ordinal();
		else
			return inst.exactStringMatch[r][l];
	}

	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
