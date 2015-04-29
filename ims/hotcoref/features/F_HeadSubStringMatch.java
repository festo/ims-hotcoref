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

public class F_HeadSubStringMatch extends AbstractPairEnumFeature<TrueFalse>{
	private static final long serialVersionUID = 1736992482622613222L;

	private final PairTargetNodeExtractor subStr;
	private final PairTargetNodeExtractor wholeSpan;
	
	
	protected F_HeadSubStringMatch(PairTargetNodeExtractor subStrNode,PairTargetNodeExtractor wholeSpanNodeExtractor) {
		super(subStrNode.ts.toString()+"HdForm"+wholeSpanNodeExtractor.ts.toString()+"SubStringMatch", TrueFalse.values());
		this.subStr=subStrNode;
		this.wholeSpan=wholeSpanNodeExtractor;
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.headSubStrMatch!=null)
			return;
		inst.headSubStrMatch=new byte[2][][];
		inst.headSubStrMatch[0]=new byte[inst.nodes.length][]; //0 means the bigger span is to the left of the head
		inst.headSubStrMatch[1]=new byte[inst.nodes.length][]; //1 means the bigger span is to the right of the head
		
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.headSubStrMatch[0][nT]=new byte[nT];
					for(int nF=0;nF<nT;++nF)
						inst.headSubStrMatch[0][nT][nF]=getByte(nF,nT,inst);
				}
				return null;
			}
		};
		Callable<Void> j2=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.headSubStrMatch[1][nT]=new byte[nT];
					for(int nF=0;nF<nT;++nF)
						inst.headSubStrMatch[1][nT][nF]=getByte(nT,nF,inst);
				}
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
		fjs.add(ThreadPoolSingleton.getInstance().submit(j2));
	}

	private byte getByte(int subIdx,int wholeIdx, Instance inst) {
//		INode sub=inst.nodes[subStr.getNodeInstIdx(nF,nT,null,inst)];
//		INode sup=inst.nodes[subStr.getOtherNodeInstIdx(nF,nT)];
//		INode sup=inst.nodes[wholeSpan.getNodeInstIdx(nF, nT, null, inst)];
		INode sub=inst.nodes[subIdx];
		INode sup=inst.nodes[wholeIdx];
		final TrueFalse t;
		if(sub.isVirtualNode() || sup.isVirtualNode())
			t=TrueFalse.VNode;
		else 
			t=getT((MNode) sub,(MNode) sup);
		return (byte) t.ordinal();
	}

	private TrueFalse getT(MNode sub, MNode sup) {
		String hdForm=sub.span.s.forms[sub.span.hd];
		Span sp=sup.span;
		for(int r=sp.start;r<=sp.end;++r)
			if(sp.s.forms[r].equals(hdForm))
				return TrueFalse.True;
		return TrueFalse.False;
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
//		int s=subStr.getNodeInstIdx(headIdx, depIdx, hotState);
//		int t=wholeSpan.getNodeInstIdx(headIdx, depIdx, hotState);
		int s=subStr.getNodeInstIdxPrecompArray(tnes);
		int t=wholeSpan.getNodeInstIdxPrecompArray(tnes);

		final int l,r,i;
		if(s<t){
			l=s;
			r=t;
			i=1;
		} else {
			l=t;
			r=s;
			i=0;
		}
		if(l<0)
			return TrueFalse.None.ordinal();
		return inst.headSubStrMatch[i][r][l];
	}

	@Override
	public boolean firstOrderFeature() {
		return subStr.firstOrder() && wholeSpan.firstOrder();
	}

}
