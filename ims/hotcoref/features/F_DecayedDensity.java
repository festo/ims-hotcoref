package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNode;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;
import ims.util.Util;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_DecayedDensity extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = -663922077059398034L;

	private final int buckets;
	private final int _bits;
	
	protected F_DecayedDensity(int buckets) {
		super("DecayDensity"+buckets);
		this.buckets=buckets;
		this._bits=Util.getBits(buckets+2);
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		//do nothing
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return _bits;
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		//do nothing
	}

	@Override
	public boolean firstOrderFeature() {
		return false;
	}

	@Override
//	int getIntValue(Instance inst, int headIdx, int depIdx, HOTState hotState) {
	int getIntValue(Instance inst, int[] tnes, HOTState hotState) {
		int headIdx=PairTargetNodeExtractor.getNodeInstIdxPrecompArray(tnes, PairTargetNode.MFrom);
		int depIdx=PairTargetNodeExtractor.getNodeInstIdxPrecompArray(tnes, PairTargetNode.MTo);
		if(headIdx==0)
			return buckets; //Placeholder for root.
		int bucket=getBucket(getRatio(hotState.leftPredecessor,hotState.chainFront[hotState.mention2chainIdx[headIdx]],depIdx),buckets);
		return bucket;
	}

	//this is stupid, it becomes 0 too quickly
//	public static double getRatio(int[] leftPredecessors,int chainFront,int dep){
//		double num=1.d;
//		for(int n=chainFront;n!=0;n=leftPredecessors[n]){
//			num*=Math.pow(0.5, dep-n);
//		}
//		double den=1.d-Math.pow(0.5, dep-1);
//		double ratio=num/den;
//		return ratio;
//	}
	
	public static double getRatio(int[] leftPredecessors,int chainFront,int dep){
		double sum=0.d;
		for(int n=chainFront;n!=0;n=leftPredecessors[n]){
			sum+=1.d/(dep-n);
		}
		return sum;
	}
	
	public static int getBucket(double d,int buckets){
		//this can be made faster... lets see if it works to begin with.
		double r=2.d/buckets;
		for(int i=0;i<buckets;++i){
			if(d>=i*r && d<=(i+1)*r)
				return i;
		}
		return buckets+1; //Placeholder for everything even larger (buckets is taken by root)
	}
}
