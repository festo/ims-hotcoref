package ims.hotcoref.data;

import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.data.CFGTree.NonTerminal;

public class CFGPath {

	public static enum PathCase {
		DS, SSFromDominatesTo, SSToDominatesFrom, SSLCA
	}
	
	public final PathCase pc;
	public final CFGNode[] fromToLCA;
	public final CFGNode[] toToLCA;
	public final CFGNode lca;
	
	public CFGPath(CFGNode from,CFGNode to){
		if(from.getSentence()!=to.getSentence()){
			pc=PathCase.DS;
			{ //From
				int fromDist=1;
				for(NonTerminal nt=from.getParent();nt!=null;nt=nt.getParent())
					fromDist++;
				fromToLCA=new CFGNode[fromDist];
				fromToLCA[0]=from;
				int f=1;
				for(NonTerminal nt=from.getParent();nt!=null;nt=nt.getParent())
					fromToLCA[f++]=nt;
			}
			{ //To
				int toDist=1;
				for(NonTerminal nt=to.getParent();nt!=null;nt=nt.getParent())
					toDist++;
				toToLCA=new CFGNode[toDist];
				toToLCA[0]=to;
				int t=1;
				for(NonTerminal nt=to.getParent();nt!=null;nt=nt.getParent())
					toToLCA[t++]=nt;
			}
			lca=null;
		}else if(dominates(from,to)){
			pc=PathCase.SSFromDominatesTo;
			int toDist=1;
			for(NonTerminal nt=to.getParent();nt!=from;nt=nt.getParent())
				toDist++;
			toToLCA=new CFGNode[toDist];
			toToLCA[0]=to;
			int i=1;
			for(NonTerminal nt=to.getParent();nt!=from;nt=nt.getParent())
				toToLCA[i++]=nt;
			fromToLCA=null;
			lca=null;
		}else if(dominates(to,from)){
			pc=PathCase.SSToDominatesFrom;
			int fromDist=1;
			for(NonTerminal nt=from.getParent();nt!=to;nt=nt.getParent())
				fromDist++;
			fromToLCA=new CFGNode[fromDist];
			fromToLCA[0]=from;
			int i=1;
			for(NonTerminal nt=from.getParent();nt!=to;nt=nt.getParent())
				fromToLCA[i++]=nt;
			toToLCA=null;
			lca=null;
		}else{
			pc=PathCase.SSLCA;
			NonTerminal lca=to.getParent();
			{ //TO
				int toDistToLCA=1;
				for(;!dominates(lca,from);lca=lca.getParent())
					toDistToLCA++;
				toToLCA=new CFGNode[toDistToLCA];
				toToLCA[0]=to;
				int i=1;
				for(NonTerminal nt=to.getParent();nt!=lca;nt=nt.getParent())
					toToLCA[i++]=nt;
			}
			{ //FROM
				int fromDistToLCA=1;
				for(NonTerminal nt=from.getParent();nt!=lca;nt=nt.getParent())
					fromDistToLCA++;
				fromToLCA=new CFGNode[fromDistToLCA];
				fromToLCA[0]=from;
				int i=1;
				for(NonTerminal nt=from.getParent();nt!=lca;nt=nt.getParent())
					fromToLCA[i++]=nt;
			}
			this.lca=lca;
		}
	}
	
	private static boolean dominates(CFGNode anc, CFGNode desc) {
		return desc.beg>=anc.beg && desc.end<=anc.end;
	}
}
