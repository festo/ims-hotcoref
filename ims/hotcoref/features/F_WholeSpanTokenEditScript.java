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

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_WholeSpanTokenEditScript extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = 4576135303359747637L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;
	
	private final TokenTraitExtractor tte;	
	private final int _tIdx;
	
	protected F_WholeSpanTokenEditScript(TokenTraitExtractor tte,PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"WholeSpanToken"+tte.tt.toString()+"EditScript");
		this.tte=tte;
		this._tIdx=tte.tt.ordinal();
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return tte.getBitsEST(symTab);
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(tte.tt.getESTType());
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.esTTrait==null)
			inst.esTTrait=new int[TokenTrait.values().length][][];
		if(inst.esTTrait[_tIdx]!=null)
			return;
		inst.esTTrait[_tIdx]=new int[inst.nodes.length][];
		
		for(int nT=1;nT<inst.nodes.length;++nT){
			inst.esTTrait[_tIdx][nT]=new int[nT];
			for(int nF=0;nF<nT;++nF)
				inst.esTTrait[_tIdx][nT][nF]=computeES(nF,nT,inst,symTab);
		}
		
//		for(int i=0;i<inst.edgeInts.length;++i)
//			inst.esTTrait[_tIdx][i]=computeES(inst.edgeInts[i],inst,symTab);
	}

	private int computeES(int nF,int nT, Instance inst, SymbolTable symTab) {
		INode from=inst.nodes[nF];
		INode to  =inst.nodes[nT];
		if(from.isVirtualNode())
			return AbstractSymbolMapping.getVNodeIntVal((VNode) from);
		if(to.isVirtualNode())
			return AbstractSymbolMapping.getVNodeIntVal((VNode) to);
		
		Span f=((MNode) from).span;
		Span t=((MNode) to).span;
		
		int[][] d=EditDistance.levenshteinDistanceTableIntArr(tte.getArr(f.s), f.start, f.end, tte.getArr(t.s), t.start, t.end);
		
		String s=dToString(d,tte.getArr(f.s), f.start, f.end, tte.getArr(t.s), t.start, t.end);
		int i=symTab.esTTrait[_tIdx].lookup(s);
		return i;
	}

	private String dToString(int[][] d,int[] a,int ai,int am,int[] b,int bi,int bm) {
		StringBuilder sb=new StringBuilder();
		int n=d.length;
		int m=d[0].length;
		int x=n-1;
		int y=m-1;
		while(true){
			if(d[x][y]==0)
				break;
			if(y>0 && x>0 && d[x-1][y-1]<d[x][y]){
				sb.append('\u0001').append(Integer.toString(x-1)).append((char)a[am]).append((char)b[bm]);
				--x;--y;
				--am;--bm;
				continue;
			}
			if(y>0 && d[x][y-1]<d[x][y]){
				sb.append('\u0002').append(Integer.toString(x)).append((char)b[bm]);
				--y;--bm;
				continue;
			}
			if(x>0 && d[x-1][y]<d[x][y]){
				sb.append('\u0003').append(Integer.toString(x-1)).append((char)a[am]);
				--x;--am;
				continue;
			}
			if (x>0&& y>0 && d[x-1][y-1]==d[x][y]) {
				x--; y--; --am; --bm;
				continue ;
			}
			if (x>0&&  d[x-1][y]==d[x][y]) {
				x--; --am;
				continue;
			}
			if (y>0 && d[x][y-1]==d[x][y]) {
				y--; --bm;
				continue;
			}
		}
		return sb.toString();
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int r=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int l=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int r=tse2.getNodeInstIdxPrecompArray(tnes);
		int l=tse1.getNodeInstIdxPrecompArray(tnes);
		return inst.esTTrait[_tIdx][r][l];
	}

	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
