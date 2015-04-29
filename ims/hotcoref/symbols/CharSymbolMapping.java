package ims.hotcoref.symbols;

import gnu.trove.map.TCharIntMap;
import gnu.trove.map.hash.TCharIntHashMap;

public class CharSymbolMapping extends AbstractSymbolMapping<Character> {
	private static final long serialVersionUID = -3582831060699187978L;

	private TCharIntMap map;
	
	public CharSymbolMapping(String name) {
		super(name);
		map=new TCharIntHashMap();
	}

	@Override
	public int lookup(Character sym) {
		return lookup(sym.charValue());
	}
	
	public int lookup(char sym){
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
	public int addSymbol(Character sym) {
		return addSymbol(sym.charValue());
	}
	
	public int addSymbol(char sym) {
		if(map.containsKey(sym))
			return map.get(sym);
		else
			return frozen?UNK_IDX:addSymbol(sym);
	}

}
