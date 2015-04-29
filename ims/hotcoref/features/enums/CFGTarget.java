package ims.hotcoref.features.enums;

import ims.hotcoref.data.CFGTree.CFGNode;

public enum CFGTarget {

	Node, ParentNode, GrandParentNode;
	
	public CFGNode getCFGNode(CFGNode node){
		switch(this){
		case Node: 				return node;
		case ParentNode: 		return node.getParent();
		case GrandParentNode:
			CFGNode par=node.getParent();
			if(par==null)
				return null;
			else
				return par.getParent();
		default: throw new Error("not implemneted");
		}
	}
	
}
