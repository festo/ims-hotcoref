package ims.hotcoref.mentiongraph;

import java.util.Collections;
import java.util.Map;

import ims.hotcoref.features.enums.Gender;
import ims.hotcoref.mentiongraph.VNode.VNodeType;

public class NodeFactory {

	public static VNode getVirtualNode(VNodeType vNodeType){
		switch(vNodeType){
		case GenericDocRoot:			return new GenericDocRootNode(vNodeType);
		case ClusterDiscoureNewRoot:
		case ClusterNonRefRoot:
		case ClusterSingletonRoot:
		}
		throw new Error("not implemented");
	}
	
	public static class GenericDocRootNode extends VNode {

		public GenericDocRootNode(VNodeType type) {
			super(type);
		}
		@Override
		public boolean isVirtualNode() {
			return true;
		}
		public int compareTo(INode o) {
			return INode.INODE_COMPARATOR.compare(this, o);
		}
		@Override
		public Gender getGender() {
			return Gender.Virtual;
		}
		@Override
		public String getDotNodeIdentifier() {
			return "ROOT";
		}
		
		public Map<String,String> getNodeAttributeList(){
			return Collections.emptyMap();
		}
	}
}
