package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.CFGTarget;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.symbols.AbstractSymbolMapping;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_CFGSubCat extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = -2173683979194197333L;

	private final PairTargetNodeExtractor tse;
	private final CFGTarget cfgTarget;
	private final int ord;
	
	protected F_CFGSubCat(PairTargetNodeExtractor tse,CFGTarget cfgTarget) {
		super(tse.ts.toString()+cfgTarget.toString()+"CFGSubCat");
		this.tse=tse;
		this.cfgTarget=cfgTarget;
		ord=cfgTarget.ordinal();
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.cfgSubCats.getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.Category);
		t.add(Types.CfgSubCat);
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
//		int idx=tse.getNodeInstIdx(headIdx,depIdx,hotState);
		int idx=tse.getNodeInstIdxPrecompArray(tnes);
		if(idx<0)
			return AbstractSymbolMapping.NONE_IDX;
		else
			return inst.cfgNodeSubCat[ord][idx];
	}
	
	private char[] getSymbol(MNode node,SymbolTable symTab) {
		CFGNode cfgNode=node.span.cfgNode;
		switch(cfgTarget){
		case GrandParentNode: if(cfgNode==null) break; else cfgNode=cfgNode.getParent();
		case ParentNode:      cfgNode=(cfgNode==null?null:cfgNode.getParent());
		default:
		}
		if(cfgNode==null)
			return null;
		return getCFGNodeSubCat(cfgNode,symTab);
	}
	
	private static char[] getCFGNodeSubCat(CFGNode cfgNode,SymbolTable symTab){
		List<CFGNode> children=cfgNode.getChildren();
		char[] c=new char[children.size()];
		for(int i=0,m=children.size();i<m;++i)
			c[i]=(char) symTab.cats.lookup(children.get(i).getLabel());
		return c;
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.cfgNodeSubCat==null)
			inst.cfgNodeSubCat=new int[CFGTarget.values().length][];
		if(inst.cfgNodeSubCat[ord]!=null)
			return;
		inst.cfgNodeSubCat[ord]=new int[inst.nodes.length];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				synchronized(symTab.cfgSubCats){
					for(int i=0;i<inst.nodes.length;++i)
						inst.cfgNodeSubCat[ord][i]=computeIntValue(inst.nodes[i],symTab);
					return null;
				}
			}			
		};
		l.add(j);
	}

	private int computeIntValue(INode node, SymbolTable symTab) {
		if(node instanceof VNode)
			return AbstractSymbolMapping.getVNodeIntVal((VNode) node);
		MNode mNode=(MNode) node;
		char[] sym=getSymbol(mNode,symTab);
		if(sym==null)
			return AbstractSymbolMapping.NONE_IDX;
		else
			return symTab.cfgSubCats.lookup(sym);
	}
	
	@Override
	public boolean firstOrderFeature() {
		return tse.firstOrder();
	}
}
