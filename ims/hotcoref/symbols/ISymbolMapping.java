package ims.hotcoref.symbols;

import java.io.Serializable;

public interface ISymbolMapping<T> extends Serializable {

	public int getBits();
	public int getItems();
	public String getName();
	public int lookup(T sym);
	public int addSymbol(T sym);
	public void freeze();
	
}
