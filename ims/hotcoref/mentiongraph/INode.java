package ims.hotcoref.mentiongraph;

import ims.hotcoref.features.enums.Gender;

import java.util.Comparator;
import java.util.Map;

public interface INode{

//	int getTokenTrait(SpanTokenExtractor ste, TokenTraitExtractor tte, SymbolTable symTab);
	public boolean isVirtualNode();
	String getKey();
	public String getDotNodeName(int nodeIdx);
	public String getDotNodeIdentifier();
	public Map<String,String> getNodeAttributeList();
	
	public static Comparator<INode> INODE_COMPARATOR=new Comparator<INode>(){
		public int compare(INode arg0, INode arg1) {
			if(arg0 instanceof VNode){
				if(arg1 instanceof VNode)
					return 0;  //All VNodes are equal
				else
					return -1; //arg0 is smaller if arg0 is a VNode and arg1 is not
			} else {
				if(arg1 instanceof VNode)
					return 1;  //arg1 is greater if arg1 is a VNode and arg0 is not
				return ((MNode) arg0).compareTo((MNode) arg1);
			}
		}
	};

	public Gender getGender();

//	int visitGetNodeAtomicInt(SymbolTable symbolTable, INodeAtomicFeature f);

//	public int getNodeInstanceIdx();
	
	
}
