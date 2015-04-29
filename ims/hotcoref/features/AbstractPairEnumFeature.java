package ims.hotcoref.features;

import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;
import ims.util.Util;

import java.util.Set;

public abstract class AbstractPairEnumFeature<T extends Enum<T>> extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = 3444885754024373634L;

	protected final T[] _ts;
	protected final int _mBits;
	
	protected AbstractPairEnumFeature(String name,T[] ts) {
		super(name);
		this._ts=ts;
		this._mBits=Util.getBits(_ts.length);
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return _mBits;
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		return; //Do nothing, we need no symbol types
	}
}
