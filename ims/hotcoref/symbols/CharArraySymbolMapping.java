package ims.hotcoref.symbols;

import gnu.trove.map.hash.TObjectIntHashMap;

public class CharArraySymbolMapping extends AbstractSymbolMapping<char[]> implements ISymbolMapping<char[]> {
	private static final long serialVersionUID = -3138896695981752534L;

	private final TObjectIntHashMap<String> map;

	public CharArraySymbolMapping(String name) {
		super(name);
		map=new TObjectIntHashMap<String>();
	}

	@Override
	public int addSymbol(char[] sym) {
		if(frozen)
			throw new UnsupportedOperationException("Cannot add to symbol table if it is already frozen.");
		String strSym=charArr2String(sym);
		if(map.containsKey(strSym))
			return map.get(strSym);
		else {
			map.put(strSym, next);
			return next++;
		}
	}
	
	@Override
	public int lookup(char[] sym) {
		String strSym=charArr2String(sym);
		if(map.containsKey(strSym))
			return map.get(strSym);
		else
			return frozen?UNK_IDX:addSymbol(sym);
	}
	
	private static String charArr2String(char[] charArray){
		return new String(charArray);
	}
	
}
