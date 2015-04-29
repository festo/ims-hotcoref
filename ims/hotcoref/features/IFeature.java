package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface IFeature extends Serializable {

	public String getName();
	public void addSymbolTypes(Set<Types> t);
//	public int fillLongs(long[] sink,int next,int t,SymbolTable symTab,int bitsT,Instance inst,int headIdx,int depIdx,HOTState hotState);
	public int fillLongs(long[] sink, int next, int i, SymbolTable symTab,int _bitsFeatureTypes,Instance inst,int[] tnes,HOTState hotState);
	public int getBits(SymbolTable symTab);
	public void XfillFillInstanceJobs(Instance inst,SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs);
	public boolean firstOrderFeature();
	
	
}
