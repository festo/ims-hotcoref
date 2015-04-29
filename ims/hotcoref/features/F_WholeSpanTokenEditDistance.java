package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

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

public class F_WholeSpanTokenEditDistance <T extends Enum<T> & IBuckets<T>> extends AbstractPairEnumFeature<T> {
	private static final long serialVersionUID = -8200954627388172086L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;

	
	private final TokenTraitExtractor tte;
	private final int _bIdx;
	private final int _tIdx;

	
	protected F_WholeSpanTokenEditDistance(TokenTraitExtractor tte, T[] ts,PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"WholeSpanToken"+tte.tt.toString()+"EditDistance"+ts[0].getClass().getSimpleName(), ts);
		this.tte=tte;
		this._bIdx=IBuckets.bIdxMap.indexOf(ts[0].getClass());
		this._tIdx=tte.tt.ordinal();
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.tokenTraitWholeSpanEditDistance==null)
			inst.tokenTraitWholeSpanEditDistance=new byte[IBuckets.bIdxMap.size()][TokenTrait.values().length][][];
		if(inst.tokenTraitWholeSpanEditDistance[_bIdx][_tIdx]!=null)
			return;
		inst.tokenTraitWholeSpanEditDistance[_bIdx][_tIdx]=new byte[inst.nodes.length][];
		//XXX I haven't tested this (yet). Make a callable and sort it out when tested.
		for(int nT=1;nT<inst.nodes.length;++nT){
			inst.tokenTraitWholeSpanEditDistance[_bIdx][_tIdx][nT]=new byte[nT];
			for(int nF=0;nF<nT;++nF)
				inst.tokenTraitWholeSpanEditDistance[_bIdx][_tIdx][nT][nF]=getByte(nF,nT,inst);
		}
//		for(int i=0;i<inst.edgeInts.length;++i)
//			inst.tokenTraitWholeSpanEditDistance[_bIdx][_tIdx][i]=getByte(inst.edgeInts[i],inst);
	}

	private byte getByte(int nF,int nT, Instance inst) {
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

	private T getBucket(Span f, Span t) {
		int[] a=tte.getArr(f.s);
		int[] b=tte.getArr(t.s);
		int editDist=EditDistance.levenshteinDistanceIntArr(a, f.start, f.end, b, t.start, t.end);
		return _ts[0].getBucket(editDist);
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int r=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int l=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int r=tse2.getNodeInstIdxPrecompArray(tnes);
		int l=tse1.getNodeInstIdxPrecompArray(tnes);
		return inst.tokenTraitWholeSpanEditDistance[_bIdx][_tIdx][r][l];
	}

	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
