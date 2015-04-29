package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.symbols.SymbolTable;

public abstract class AbstractAtomicPairFeature extends AbstractSinglePairFeature {
	private static final long serialVersionUID = 2609807728712249537L;

	protected AbstractAtomicPairFeature(String name) {
		super(name);
	}

	abstract int getIntValue(Instance inst,int[] tnes,HOTState hotState);
	
	@Override
	public long appendBits(long initVal,Instance inst, SymbolTable symTab,int[] tnes,HOTState hotState){
		int bitsNeeded=getBits(symTab);
		int value=getIntValue(inst,tnes,hotState);
		if(value<0)
			return -1l;
		initVal<<=bitsNeeded;
		initVal|=value;
		return initVal;
	}
	
}
