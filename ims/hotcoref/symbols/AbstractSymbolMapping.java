package ims.hotcoref.symbols;

import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.mentiongraph.VNode.VNodeType;
import ims.util.Util;

public abstract class AbstractSymbolMapping<T> implements ISymbolMapping<T>{
	private static final long serialVersionUID = 1563386098615065614L;
	
	
	public static final int UNK_IDX = 0;
	public static final int NONE_IDX = 1;
	
	private static final int VNODE_OFFSET=2; //increment this if more dummies are added above
	
	protected int next;
	protected boolean frozen;
	protected final String name;
	protected int _fBits=-1;

	public AbstractSymbolMapping(String name) {
		this.name=name;
		this.next=VNODE_OFFSET;
		//Then reserve space for VNodes:
		this.next+=VNodeType.values().length;
		this.frozen=false;
	}

	@Override
	public int getItems() {
		return next;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void freeze() {
		if(frozen)
			throw new UnsupportedOperationException("Cannot call freeze twice");
		_fBits=getBits();
		frozen=true;
	}

	@Override
	public int getBits() {
		if(frozen)
			return _fBits;
		else
			return Util.getBits(getItems());
	}

	public static int getVNodeIntVal(VNode vNode){
		return VNODE_OFFSET+vNode.type.ordinal();
	}
}
