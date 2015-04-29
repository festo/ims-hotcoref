package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class AbstractSingleSinglePairFeatureBigram extends AbstractSinglePairFeature {
	private static final long serialVersionUID = 5140353861356961154L;

	private final AbstractSinglePairFeature apf1;
	private final AbstractSinglePairFeature apf2;
	
	protected AbstractSingleSinglePairFeatureBigram(String name,AbstractSinglePairFeature apf1,AbstractSinglePairFeature apf2) {
		super(name);
		this.apf1=apf1;
		this.apf2=apf2;
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		apf1.addSymbolTypes(t);
		apf2.addSymbolTypes(t);
	}

	@Override
	public long appendBits(long initVal,Instance inst,SymbolTable symTab,int[] tnes,HOTState hotState) {
		long l1=apf1.appendBits(initVal,inst,symTab,tnes,hotState);
		if(l1<0)
			return -1;
		long l2=apf2.appendBits(l1,inst,symTab,tnes,hotState);
		return l2;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return apf1.getBits(symTab)+apf2.getBits(symTab);
	}

	@Override
	public void XfillFillInstanceJobs(Instance inst, SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs) {
		apf1.XfillFillInstanceJobs(inst, symTab, l, fjs);
		apf2.XfillFillInstanceJobs(inst, symTab, l, fjs);
	}

	@Override
	public boolean firstOrderFeature() {
		return apf1.firstOrderFeature() && apf2.firstOrderFeature();
	}
}
