package ims.hotcoref.symbols;

import gnu.trove.map.hash.TIntIntHashMap;

public class IntSymbolMapping extends AbstractSymbolMapping<Integer> {
	private static final long serialVersionUID = 7665423583551838114L;

	private final TIntIntHashMap m;
	
	public IntSymbolMapping(String name) {
		super(name);
		this.m=new TIntIntHashMap();
	}

	@Override
	public int lookup(Integer sym) {
		return lookup(sym.intValue());
	}
	
	public int lookup(int sym){
		if(m.containsKey(sym))
			return m.get(sym);
		else
			return frozen?UNK_IDX:addSymbol(sym);
	}
	
	
	public int addSymbol(int sym){
		if(frozen)
			throw new UnsupportedOperationException("Cannot add to symbol table if it is already frozen.");
		if(m.containsKey(sym))
			return m.get(sym);
		else {
			m.put(sym, next);
			return next++;
		}
	}

	@Override
	public int addSymbol(Integer sym) {
		return addSymbol(sym.intValue());
	}

}
