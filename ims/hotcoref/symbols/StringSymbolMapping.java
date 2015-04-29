package ims.hotcoref.symbols;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class StringSymbolMapping extends AbstractSymbolMapping<String> implements ISymbolMapping<String> {
	private static final long serialVersionUID = 7800294683606259865L;
	
	private TObjectIntMap<String> map; 

	protected StringSymbolMapping(String name){
		super(name);
		this.map=new TObjectIntHashMap<String>();
	}
	
	@Override
	public int addSymbol(String sym){
		if(frozen)
			throw new UnsupportedOperationException("Cannot add to symbol table if it is already frozen.");
		if(map.containsKey(sym))
			return map.get(sym);
		else {
			map.put(sym, next);
			return next++;
		}
	}
	
	@Override
	public int lookup(String sym){
		if(map.containsKey(sym))
			return map.get(sym);
		else
			return frozen?UNK_IDX:addSymbol(sym);
	}

//	@Override
	public String mapping2String() {
		return map.toString();
	}
}
