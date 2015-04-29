package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.symbols.SymbolTable;

public abstract class AbstractMultiPairFeature implements IFeature {
	private static final long serialVersionUID = 3960752661706749121L;
	
	private final String name;

	protected AbstractMultiPairFeature(String name){
		this.name=name;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
//	public int fillLongs(long[] sink, int next, int t, SymbolTable symTab, int bitsT, Instance inst,int headIdx,int depIdx,HOTState hotState) {
	public int fillLongs(long[] sink, int next, int t, SymbolTable symTab,int bitsT,Instance inst,int[] tnes,HOTState hotState) {
//		long[] l=getLongArr(symTab,inst,headIdx,depIdx,hotState);
		long[] l=getLongArr(symTab,inst,tnes,hotState);
		if(l==null)
			return next;
		for(long value:l){
			sink[next]=value;
			sink[next]<<=bitsT;
			sink[next]|=t;
			++next;
		}
		return next;
	}
	
//	abstract long[] getLongArr(SymbolTable symTab,Instance inst,int headIdx,int depIdx,HOTState hotState);
	
	abstract long[] getLongArr(SymbolTable symTab,Instance inst,int[] tnes,HOTState hotState);
	
	public String toString(){
		return getName();
	}
}
