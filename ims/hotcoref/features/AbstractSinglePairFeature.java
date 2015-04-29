package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.symbols.SymbolTable;

public abstract class AbstractSinglePairFeature implements IFeature {
	private static final long serialVersionUID = -6859960175129640755L;
	
	private final String name;
	
	protected AbstractSinglePairFeature(String name){
		this.name=name;
	}
	
	@Override
	public String getName(){
		return name;
	}

	@Override
	public int fillLongs(long[] sink, int next, int t, SymbolTable symTab,int bitsT,Instance inst,int[] tnes,HOTState hotState) {
		sink[next]=appendBits(0l,inst, symTab,tnes,hotState);
		if(sink[next]<0)
			return next;
		sink[next]<<=bitsT;
		sink[next]|=t;
		return next+1;
	}
	
	abstract long appendBits(long initVal,Instance instance, SymbolTable symTab,int[] tnes,HOTState hotState);
	
	public String toString(){
		return getName();
	}
}
