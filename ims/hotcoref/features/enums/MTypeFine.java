package ims.hotcoref.features.enums;

import ims.hotcoref.mentiongraph.INode;

public enum MTypeFine implements IMType<MTypeFine> {
	//XXX todo
	none,
	s,f,dg;

	@Override
	public MTypeFine getType(INode node) {
		// TODO Auto-generated method stub
		throw new Error("not implemented");
	}

	@Override
	public int getNone() {
		return none.ordinal();
	}

}
