package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.IBuckets;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.features.extractors.TokenTrait;
import ims.hotcoref.features.extractors.TokenTraitExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.EditDistance;
import ims.util.ThreadPoolSingleton;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_WholeSpanTraitEditDistance <T extends Enum<T> & IBuckets<T>> extends AbstractPairEnumFeature<T> {
	private static final long serialVersionUID = 5433530843696030132L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;

	
	private final TokenTraitExtractor tte;
	private final int _bIdx;
	private final int _tIdx;
	
	protected F_WholeSpanTraitEditDistance(TokenTraitExtractor tte,T[] values,PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"WholeSpan"+tte.tt.toString()+"EditDistance"+values[0].getClass().getSimpleName(),values);
		this.tte=tte;
		this._bIdx=IBuckets.bIdxMap.indexOf(values[0].getClass());
		this._tIdx=tte.tt.ordinal();
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.traitWholeSpanEditDistance==null)
			inst.traitWholeSpanEditDistance=new byte[IBuckets.bIdxMap.size()][TokenTrait.values().length][][];
		if(inst.traitWholeSpanEditDistance[_bIdx][_tIdx]!=null)
			return;
		inst.traitWholeSpanEditDistance[_bIdx][_tIdx]=new byte[inst.nodes.length][];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.traitWholeSpanEditDistance[_bIdx][_tIdx][nT]=new byte[nT];
					for(int nF=0;nF<nT;++nF)
						inst.traitWholeSpanEditDistance[_bIdx][_tIdx][nT][nF]=getEDByte(nF,nT,inst);
				}
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	private byte getEDByte(int nF,int nT, Instance inst) {
		final T t;
		INode from=inst.nodes[nF];
		INode to  =inst.nodes[nT];
		if(from.isVirtualNode() || to.isVirtualNode())
			t=_ts[0].getVNodeBucket();
		else {
			MNode f=(MNode) from;
			MNode tt=(MNode) to;
			t=getBucket(f.span,tt.span);
		}
		return (byte) t.ordinal();
	}

	private T getBucket(Span s1, Span s2) {
		String st1=getS(s1,tte);
		String st2=getS(s2,tte);
		int editDist=EditDistance.levenshteinDistance(st1, st2);
		return _ts[0].getBucket(editDist);
	}

	static String getS(Span s,TokenTraitExtractor tte) {
		StringBuilder sb=new StringBuilder(tte.getTrait(s.s, s.start));
		for(int r=s.start+1;r<s.end;++r)
			sb.append(' ').append(tte.getTrait(s.s, r));
		return sb.toString();
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int r=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int l=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int r=tse2.getNodeInstIdxPrecompArray(tnes);
		int l=tse1.getNodeInstIdxPrecompArray(tnes);
		return inst.traitWholeSpanEditDistance[_bIdx][_tIdx][r][l];
	}
	
	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
