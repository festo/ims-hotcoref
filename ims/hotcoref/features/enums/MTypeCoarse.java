package ims.hotcoref.features.enums;

import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;

public enum MTypeCoarse implements IMType<MTypeCoarse> {

	Proper, Common, Pronoun, Virtual, None;

	@Override
	public MTypeCoarse getType(INode node) {
		if(node.isVirtualNode())
			return Virtual;
		MNode mNode=(MNode) node;
		if(mNode.span.isProperName)
			return Proper;
		else if(mNode.span.isPronoun)
			return Pronoun;
		else
			return Common;
	}

	@Override
	public int getNone() {
		return None.ordinal();
	}
	
}
