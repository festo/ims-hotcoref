package ims.hotcoref.mentiongraph;

public abstract class VNode implements INode {

	public final VNodeType type;
	
	public VNode(VNodeType type){
		this.type=type;
	}
	
	public enum VNodeType {
		GenericDocRoot,
		ClusterDiscoureNewRoot,
		ClusterNonRefRoot,
		ClusterSingletonRoot
	}
	
	public static final char[][] VNODE_CHARARR_PLACEHOLDERS;
	static {
		int typeCount=VNodeType.values().length;
		VNODE_CHARARR_PLACEHOLDERS=new char[typeCount][];
		for(int i=0;i<typeCount;++i)
			VNODE_CHARARR_PLACEHOLDERS[i]=new char[]{'v',(char) i};
	}
	
	public int hashcode(){
		return type.ordinal()+37;
	}
	public boolean equals(Object other){
		if(!(other instanceof VNode))
			return false;
		return ((VNode) other).type==this.type;
	}
	public String getDotNodeName(int nodeIdx){
		return getKey();
	}
	@Override
	public String getKey() {
		return type.toString();
	}

//	@Override
//	public int visitGetNodeAtomicInt(SymbolTable symbolTable, INodeAtomicFeature f) {
//		return 2+type.ordinal();
//	}
}
