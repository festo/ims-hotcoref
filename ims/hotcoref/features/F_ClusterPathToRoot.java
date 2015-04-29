package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNode;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_ClusterPathToRoot extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = -3500142816338569656L;

	private final int maxLength;
	private final boolean linear;
	private final int _mBits;
	
	protected F_ClusterPathToRoot(boolean linear,int maxLength) {
		super((linear?"Linear":"")+"ClusterPath"+maxLength);
		this.maxLength=maxLength;
		this.linear=linear;
		this._mBits=maxLength<<1;
		if(maxLength>15)
			throw new RuntimeException("Can't do cluster length with more than 15"); //XXX to fix this, change the pack method to return a long.
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return _mBits;
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {}

	@Override
	public boolean firstOrderFeature() {
		return false;
	}

	@Override
	int getIntValue(Instance inst, int[] tnes, HOTState hotState) {
		int head=PairTargetNodeExtractor.getNodeInstIdxPrecompArray(tnes, PairTargetNode.MFrom);
		int[] t=(linear?getPathLinear(hotState,inst,head):getPathTree(hotState,inst,head));
		int r=pack(t);
		return r;
	}
	
	int[] getPathLinear(HOTState hotState,Instance inst,int head){
		int[] r=new int[maxLength];
		int cix=hotState.mention2chainIdx[head];
		int ix=hotState.chainFront[cix];
		int q=0;
		while(q<r.length && ix!=-1){
			r[q++]=inst.mentionTypes[1][ix];
			ix=hotState.leftPredecessor[ix];
		}
		while(q<r.length)
			r[q++]=3;
		return r;
	}
	
	int[] getPathTree(HOTState hotState,Instance inst,int head){
		int[] r=new int[maxLength];
		int ix=head;
		int q=0;
		while(q<r.length && ix!=0){
			r[q++]=inst.mentionTypes[1][ix];
			ix=hotState.heads[ix];
		}
		while(q<r.length)
			r[q++]=3;
		return r;
	}
	
	public int pack(int[] p){
		int r=0;
		for(int i:p){
			r|=i;
			r<<=2;
		}
		r>>=2;
		return r;
	}
	
}
