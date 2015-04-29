package ims.hotcoref.headrules;

import java.util.List;
import java.util.Map;

import ims.hotcoref.Options;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.headrules.HeadRules.Direction;
import ims.hotcoref.headrules.HeadRules.Rule;

public class HeadFinder {

	private final Map<String,HeadRules> m;
	
	public HeadFinder(Map<String,HeadRules> m){
		this.m=m;
	}
	
	public int findHead(Sentence s,CFGNode node){
		if(node==null)
			return -1;
		if(node.beg==node.end)
			return node.beg;
		HeadRules hr=m.get(node.getLabel());
		if(hr==null){
			if(Options.DEBUG)
				System.out.println("Couldnt find head rules for label: "+node.getLabel());
			return -1;
//			System.exit(1);
		}
		for(Rule r:hr.rules){
			int h=findHead(r,s,node);
			if(h>0)
				return h;
//			else
//				return node.end;
		}
		return node.end;
//		throw new RuntimeException("Failed to find head.");
	}
	
	public int findHead(Sentence s,int beg,int end){
		throw new Error("not implemented");
	}
	

	private int findHead(Rule r, Sentence s, CFGNode node) {
		if(node.beg==node.end)
			return node.beg;
		List<CFGNode> children=node.getChildren();
		if(r.d==Direction.LeftToRight){
			for(CFGNode c:children)
				if(r.headPOSPattern.matcher(c.getLabel()).matches())
					return findHead(s,c);
			
		} else {
			for(int k=children.size()-1;k>=0;--k){
				CFGNode n=children.get(k);
				if(r.headPOSPattern.matcher(n.getLabel()).matches())
					return findHead(s,n);
			}
		}
		return 0;
	}
	
}
