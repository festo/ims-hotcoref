package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_Genre extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = -5989928437637036426L;

	protected F_Genre() {
		super("Genre");
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.genre.getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.Genre);
	}

	@Override
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		return inst.genre;
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.genre==-1)
			throw new Error("!");
	}
	
	@Override
	public boolean firstOrderFeature() {
		return true;
	}
}
