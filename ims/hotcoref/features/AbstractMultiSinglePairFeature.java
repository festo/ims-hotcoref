package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class AbstractMultiSinglePairFeature extends AbstractMultiPairFeature {
	private static final long serialVersionUID = 1908772698278494342L;

	private final AbstractMultiPairFeature af1;
	private final AbstractSinglePairFeature af2;
	
	public AbstractMultiSinglePairFeature(AbstractMultiPairFeature af1,AbstractSinglePairFeature af2){
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
//		long[] a1=af1.getLongArr(symTab,inst,headIdx,depIdx,hotState);
		long[] a1=af1.getLongArr(symTab,inst,tnes,hotState);
		if(a1==null)
			return null;
//		long a2=af2.appendBits(0,inst,symTab,headIdx,depIdx,hotState);
		long a2=af2.appendBits(0,inst,symTab,tnes,hotState);
		int bits=af2.getBits(symTab);
		for(int i=0;i<a1.length;++i){
			a1[i]<<=bits;
			a1[i]|=a2;
		}
		return a1;
	}

	@Override
	public boolean firstOrderFeature() {
		return af1.firstOrderFeature() && af2.firstOrderFeature();
	}
}
