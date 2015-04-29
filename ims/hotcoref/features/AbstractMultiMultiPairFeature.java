package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class AbstractMultiMultiPairFeature extends AbstractMultiPairFeature {
	private static final long serialVersionUID = -581407656408443813L;

	private final AbstractMultiPairFeature af1;
	private final AbstractMultiPairFeature af2;
	
	public AbstractMultiMultiPairFeature(AbstractMultiPairFeature af1,AbstractMultiPairFeature af2){
		super(FeatureSet.getCanonicalName(af1.getName(),af2.getName()));
		this.af1=af1;
		this.af2=af2;
	}
	
	@Override
	public void addSymbolTypes(Set<Types> t) {
		af1.addSymbolTypes(t);
		af2.addSymbolTypes(t);
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		af1.XfillFillInstanceJobs(inst, symTab, l, fjs);
		af2.XfillFillInstanceJobs(inst, symTab, l, fjs);
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return af1.getBits(symTab)+af2.getBits(symTab);
	}

	@Override
//	long[] getLongArr(SymbolTable symTab,Instance inst,int headIdx,int depIdx,HOTState hotState) {
	long[] getLongArr(SymbolTable symTab,Instance inst,int[] tnes,HOTState hotState) {
//		long[] l1=af1.getLongArr(symTab,inst,headIdx,depIdx,hotState);
//		long[] l2=af2.getLongArr(symTab,inst,headIdx,depIdx,hotState);
		long[] l1=af1.getLongArr(symTab,inst,tnes,hotState);
		long[] l2=af2.getLongArr(symTab,inst,tnes,hotState);
		if(l1==null || l2==null)
			return null;
		long[] out=new long[l1.length*l2.length];
		int r=0;
		for(long a:l1){
			a<<=af2.getBits(symTab);
			for(long b:l2)
				out[r++]=(a|b);
		}
		return out;
	}

	@Override
	public boolean firstOrderFeature() {
		return af1.firstOrderFeature() && af2.firstOrderFeature();
	}
}
