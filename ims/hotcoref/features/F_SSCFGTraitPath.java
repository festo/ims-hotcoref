package ims.hotcoref.features;

import ims.hotcoref.data.CFGPath;
import ims.hotcoref.data.Instance;
import ims.hotcoref.data.CFGPath.PathCase;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.features.extractors.TokenTrait;
import ims.hotcoref.features.extractors.TokenTraitExtractor;
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

public class F_SSCFGTraitPath extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = 2128887533683889804L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;

	
	private final TokenTraitExtractor tte;
	private final int traitOrd;
	
	protected F_SSCFGTraitPath(TokenTraitExtractor tte,PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"CFGSS"+tte.tt.toString()+"Path");
		this.tte=tte;
		this.traitOrd=tte.tt.ordinal();
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.ssTraitPaths[traitOrd].getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.Pos);
		t.add(Types.SSPosPath);
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int r=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int l=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int r=tse2.getNodeInstIdxPrecompArray(tnes);
		int l=tse1.getNodeInstIdxPrecompArray(tnes);
		return inst.cfgSSTraitPath[traitOrd][r][l];
	}
	
	@Override
	public void XfillFillInstanceJobs(final Instance inst, final SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.cfgSSTraitPath==null)
			inst.cfgSSTraitPath=new int[TokenTrait.values().length][][];
		if(inst.cfgSSTraitPath[traitOrd]!=null)
			return;
		inst.cfgSSTraitPath[traitOrd]=new int[inst.nodes.length][];
		
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.cfgSSTraitPath[traitOrd][nT]=new int[nT];
					for(int nF=0;nF<nT;++nF)
						inst.cfgSSTraitPath[traitOrd][nT][nF]=computeInt(nF,nT,symTab,inst);
				}
				return null;
			}
		};
		l.add(j);
	}
	
	private int computeInt(int nF, int nT, SymbolTable symTab,Instance inst) {
		INode from=inst.nodes[nF];
		INode to=inst.nodes[nT];
		if(from instanceof VNode)
			return AbstractSymbolMapping.getVNodeIntVal((VNode) from);
		MNode mFrom=(MNode) from;
		MNode mTo=(MNode) to;
		if(mFrom.span.s.sentenceIndex!=mTo.span.s.sentenceIndex)
			return AbstractSymbolMapping.NONE_IDX;
		char[] c=getCharArray(mFrom,mTo,symTab);
		int i=symTab.ssTraitPaths[traitOrd].lookup(c);
		return i;
	}

	private static final char[] BOTH_NULL=new char[]{'0'};
	private static final char[] TO_NULL=new char[]{'1'};
	private static final char[] FROM_NULL=new char[]{'2'};
	private char[] getCharArray(MNode from, MNode to, SymbolTable symTab) {
		if(from.span.cfgNode==null)
			if(to.span.cfgNode==null)
				return BOTH_NULL;
			 else 
				return FROM_NULL;			
		 else if(to.span.cfgNode==null)
			return TO_NULL;
		CFGPath p=from.span.cfgNode.getPathTo(to.span.cfgNode);
		int[] arr=tte.getArr(from.span.s);
		if(p.pc==PathCase.SSToDominatesFrom){
			char[] c=new char[2*p.fromToLCA.length+1];
			int i=c.length-1;
			for(int j=0;j<p.fromToLCA.length;++j){
				c[i--]=(char) arr[p.fromToLCA[j].getHead()];
				c[i--]='D';
			}
			c[i]=(char) arr[to.span.cfgNode.getHead()];
			if(i!=0)
				throw new Error("problem here");
			return c;
		} else if(p.pc==PathCase.SSFromDominatesTo){
			char[] c=new char[2*p.toToLCA.length+1];
			int i=0;
			for(int j=0;j<p.toToLCA.length;++j){
				c[i++]=(char) arr[p.toToLCA[j].getHead()];
				c[i++]='U';
			}
			c[i]=(char) arr[to.span.cfgNode.getHead()];
			if(i!=(c.length-1))
				throw new Error("problem here");
			return c;
		} else if(p.pc==PathCase.SSLCA){
			//toDistToLCA*2+fromDistToLCA*2+1];
			char[] c=new char[p.fromToLCA.length*2+p.toToLCA.length*2+1];
			int i=0;
			for(int j=0;j<p.toToLCA.length;++j){
				c[i++]=(char) arr[p.toToLCA[j].getHead()];
				c[i++]='U';
			}
			c[i]=(char) arr[p.lca.getHead()];
			int r=c.length-1;
			for(int j=0;j<p.fromToLCA.length;++j){
				c[r--]=(char) arr[p.fromToLCA[j].getHead()];
				c[r--]='D';
			}
			if(i!=r)
				throw new Error("problem here");
			return c;
		}
		throw new Error("should be dealt with outside of this method");
	}

	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
