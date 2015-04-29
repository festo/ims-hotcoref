package ims.hotcoref.features;

import ims.hotcoref.data.CFGPath;
import ims.hotcoref.data.Instance;
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

public class F_DSCFGPath extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = -8252921844035234046L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;

	
	protected F_DSCFGPath(PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"CFGDSPath");
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.dsCFGPaths.getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.Category);
		t.add(Types.DSCFGPath);
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst, final SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.cfgDSPath!=null)
			return;
		inst.cfgDSPath=new int[inst.nodes.length][];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.cfgDSPath[nT]=new int[nT];
					for(int nF=0;nF<nT;++nF)
						inst.cfgDSPath[nT][nF]=computeInt(nF,nT,symTab,inst);
				}
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
//		int r=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int l=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
		int r=tse2.getNodeInstIdxPrecompArray(tnes);
		int l=tse1.getNodeInstIdxPrecompArray(tnes);
		return inst.cfgDSPath[r][l];
	}
	private static final char[] BOTH_NULL=new char[]{'0'};
	private static final char[] TO_NULL=new char[]{'1'};
	private static final char[] FROM_NULL=new char[]{'2'};
	private int computeInt(int nF,int nT, SymbolTable symTab,Instance inst) {
		INode from=inst.nodes[nF];
		INode to=inst.nodes[nT];
		if(from instanceof VNode)
			return AbstractSymbolMapping.getVNodeIntVal((VNode) from);
		MNode mFrom=(MNode) from;
		MNode mTo=(MNode) to;
		if(mFrom.span.s.sentenceIndex==mTo.span.s.sentenceIndex)
			return AbstractSymbolMapping.NONE_IDX;
		char[] c=getCharArray(mFrom,mTo,symTab);
		int i=symTab.dsCFGPaths.lookup(c);
		return i;

	}

	private char[] getCharArray(MNode from, MNode to, SymbolTable symTab) {
		if(from.span.cfgNode==null)
			if(to.span.cfgNode==null)
				return BOTH_NULL;
			 else 
				return FROM_NULL;			
		 else if(to.span.cfgNode==null)
			return TO_NULL;
		CFGPath p=from.span.cfgNode.getPathTo(to.span.cfgNode);
		char[] c=new char[p.fromToLCA.length*2+p.toToLCA.length*2-3];
		int i=0;
		for(int j=0,m=p.toToLCA.length-1;j<m;++j){
			c[i++]=(char) symTab.cats.lookup(p.toToLCA[j].getLabel());
			c[i++]='U';
		}
//		c[i]='0'; //Not needed --- it's always going to be U0D
		int r=c.length-1;
		for(int j=0,m=p.fromToLCA.length-1;j<m;++j){
			c[r--]=(char) symTab.cats.lookup(p.fromToLCA[j].getLabel());
			c[r--]='D';
		}
		if(i!=r)
			throw new Error("problem here");
		return c;
	}
	
	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
