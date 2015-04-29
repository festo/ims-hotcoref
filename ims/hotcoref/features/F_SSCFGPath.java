package ims.hotcoref.features;

import ims.hotcoref.data.CFGPath;
import ims.hotcoref.data.Instance;
import ims.hotcoref.data.CFGPath.PathCase;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.symbols.AbstractSymbolMapping;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;
import ims.util.ThreadPoolSingleton;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_SSCFGPath extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = -5837921187107299602L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;

	
	protected F_SSCFGPath(PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"CFGSSPath");
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.ssCFGPaths.getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.Category);
		t.add(Types.SSCFGPath);
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int q=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int p=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int q=tse2.getNodeInstIdxPrecompArray(tnes);
		int p=tse1.getNodeInstIdxPrecompArray(tnes);
		int r=Math.max(p, q);
		int l=Math.min(p, q);
		if(l<0)
			return AbstractSymbolMapping.NONE_IDX;
		else
			return inst.cfgSSPath[r][l];
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.cfgSSPath!=null)
			return;
		inst.cfgSSPath=new int[inst.nodes.length][];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.cfgSSPath[nT]=new int[nT];
					for(int nF=0;nF<nT;++nF)
						inst.cfgSSPath[nT][nF]=computeInt(nF,nT,symTab,inst);
				}
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
//		l.add(j);
	}

	private int computeInt(int nF,int nT, SymbolTable symTab,Instance inst) {
		INode from=inst.nodes[nF];
		INode to=inst.nodes[nT];
		if(from instanceof VNode)
			return AbstractSymbolMapping.getVNodeIntVal((VNode) from);
		MNode mFrom=(MNode) from;
		MNode mTo=(MNode) to;
		if(mFrom.span.s.sentenceIndex!=mTo.span.s.sentenceIndex)
			return AbstractSymbolMapping.NONE_IDX;
		char[] c=getCharArray(mFrom,mTo,symTab);
		int i=symTab.ssCFGPaths.lookup(c);
		return i;
	}
	
	private static final char[] BOTH_NULL=new char[]{'0'};
	private static final char[] TO_NULL=new char[]{'1'};
	private static final char[] FROM_NULL=new char[]{'2'};


	private char[] getCharArray(MNode from,MNode to,SymbolTable symTab){
		if(from.span.cfgNode==null)
			if(to.span.cfgNode==null)
				return BOTH_NULL;
			 else 
				return FROM_NULL;			
		 else if(to.span.cfgNode==null)
			return TO_NULL;

		CFGPath p=from.span.cfgNode.getPathTo(to.span.cfgNode);
		if(p.pc==PathCase.SSToDominatesFrom){
			char[] c=new char[2*p.fromToLCA.length+1];
			int i=c.length-1;
			for(int j=0;j<p.fromToLCA.length;++j){
				c[i--]=(char) symTab.cats.lookup(p.fromToLCA[j].getLabel());
				c[i--]='D';
			}
			c[i]=(char) symTab.cats.lookup(to.span.cfgNode.getLabel());
			if(i!=0)
				throw new Error("problem here");
			return c;
		} else if(p.pc==PathCase.SSFromDominatesTo){
			char[] c=new char[2*p.toToLCA.length+1];
			int i=0;
			for(int j=0;j<p.toToLCA.length;++j){
				c[i++]=(char) symTab.cats.lookup(p.toToLCA[j].getLabel());
				c[i++]='U';
			}
			c[i]=(char) symTab.cats.lookup(to.span.cfgNode.getLabel());
			if(i!=(c.length-1))
				throw new Error("problem here");
			return c;
		} else if(p.pc==PathCase.SSLCA){
			//toDistToLCA*2+fromDistToLCA*2+1];
			char[] c=new char[p.fromToLCA.length*2+p.toToLCA.length*2+1];
			int i=0;
			for(int j=0;j<p.toToLCA.length;++j){
				c[i++]=(char) symTab.cats.lookup(p.toToLCA[j].getLabel());
				c[i++]='U';
			}
			c[i]=(char) symTab.cats.lookup(p.lca.getLabel());
			int r=c.length-1;
			for(int j=0;j<p.fromToLCA.length;++j){
				c[r--]=(char) symTab.cats.lookup(p.fromToLCA[j].getLabel());
				c[r--]='D';
			}
			if(i!=r)
				throw new Error("problem here");
			return c;
		}
		throw new Error("should be dealt with outside of this method"); 
	}
	
//	private char[] getCharArray(MNode from,MNode to,SymbolTable symTab){
//		if(from.span.cfgNode==null){
//			if(to.span.cfgNode==null){
//				return BOTH_NULL;
//			} else {
//				return FROM_NULL;
//			}
//		} else if(to.span.cfgNode==null){
//			return TO_NULL;
//		}
//		if(dominates(to.span.cfgNode,from.span.cfgNode)){ //To dominates From
//			int dist=1;
//			for(CFGNode par=from.span.cfgNode.getParent();par!=to.span.cfgNode;par=par.getParent())
//				dist++;
//			char[] c=new char[2*dist+1];
//			int i=2*dist;
//			c[i--]=(char) symTab.cats.lookup(from.span.cfgNode.getLabel());
//			for(CFGNode par=from.span.cfgNode.getParent();par!=to.span.cfgNode;par=par.getParent()){
//				c[i--]='U';
//				c[i--]=(char) symTab.cats.lookup(par.getLabel());
//			}
//			return c;
//		} else if(dominates(from.span.cfgNode,to.span.cfgNode)){ //From dominates To
//			int dist=1;
//			for(CFGNode par=to.span.cfgNode.getParent();par!=from.span.cfgNode;par=par.getParent())
//				dist++;
//			char[] c=new char[2*dist+1];
//			c[0]=(char) symTab.cats.lookup(to.span.cfgNode.getLabel());
//			int i=1;
//			for(CFGNode par=to.span.cfgNode.getParent();par!=from.span.cfgNode;par=par.getParent()){
//				c[i++]='D';
//				c[i++]=(char) symTab.cats.lookup(par.getLabel());
//			}
//			return c;
//		} else { //Neither dominates the other
//			int toDistToLCA=1;
//			CFGNode lca=to.span.cfgNode.getParent();
//			for(;!dominates(lca,from.span.cfgNode);lca=lca.getParent())
//				toDistToLCA++;
//			int fromDistToLCA=1;
//			for(CFGNode l=from.span.cfgNode.getParent();l!=lca;l=l.getParent())
//				fromDistToLCA++;
//			char[] c=new char[toDistToLCA*2+fromDistToLCA*2+1];
//			int i=0;
//			c[i++]=(char) symTab.cats.lookup(to.span.cfgNode.getLabel());
//			c[i++]='U';
//			for(CFGNode q=to.span.cfgNode.getParent();q!=lca;q=q.getParent()){
//				c[i++]=(char) symTab.cats.lookup(q.getLabel());
//				c[i++]='U';
//			}
//			c[i]=(char) symTab.cats.lookup(lca.getLabel());
//			int j=c.length-1;
//			c[j--]=(char) symTab.cats.lookup(from.span.cfgNode.getLabel());
//			c[j--]='D';
//			for(CFGNode l=from.span.cfgNode.getParent();l!=lca;l=l.getParent()){
//				c[j--]=(char) symTab.cats.lookup(l.getLabel());
//				c[j--]='D';
//			}
//			if(i!=j) //XXX this can go if it works
//				throw new Error("this is wrongly implemented");
//			return c;
//		}
//	}
//
//	private static boolean dominates(CFGNode anc, CFGNode desc) {
//		return desc.beg>=anc.beg && desc.end<=anc.end;
//	}
//
	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
