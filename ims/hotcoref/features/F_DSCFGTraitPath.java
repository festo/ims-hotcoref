package ims.hotcoref.features;

import ims.hotcoref.data.CFGPath;
import ims.hotcoref.data.Instance;
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

public class F_DSCFGTraitPath extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = -4600764928754409942L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;

	
	private final TokenTraitExtractor tte;
	private final int ord;
	
	protected F_DSCFGTraitPath(TokenTraitExtractor tte,PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"CFGDS"+tte.tt.toString()+"Path");
		this.tte=tte;
		this.ord=tte.tt.ordinal();
		this.tse1=t1;
		this.tse2=t2;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.dsTraitPaths[ord].getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		switch(tte.tt){
		case Form:	t.add(Types.DSFormPath);	break;
		case Pos:	t.add(Types.DSPosPath);		break;
		case Lemma: t.add(Types.DSLemmaPath);	break;
		case BWUV:	t.add(Types.DSBWUVPath);	break;
		default:	throw new Error("you are wrong here");
		}
		t.add(tte.tt.getType());
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
//		int r=tse2.getNodeInstIdx(headIdx, depIdx, hotState);
//		int l=tse1.getNodeInstIdx(headIdx, depIdx, hotState);
		int r=tse2.getNodeInstIdxPrecompArray(tnes);
		int l=tse1.getNodeInstIdxPrecompArray(tnes);

		return inst.cfgDSTraitPath[ord][r][l];
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst, final SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.cfgDSTraitPath==null)
			inst.cfgDSTraitPath=new int[TokenTrait.values().length][][];
		if(inst.cfgDSTraitPath[ord]!=null)
			return;
		inst.cfgDSTraitPath[ord]=new int[inst.nodes.length][];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.cfgDSTraitPath[ord][nT]=new int[nT];
					for(int nF=0;nF<nT;++nF)
						inst.cfgDSTraitPath[ord][nT][nF]=computeInt(nF,nT,symTab,inst);
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
		if(mFrom.span.s.sentenceIndex==mTo.span.s.sentenceIndex)
			return AbstractSymbolMapping.NONE_IDX;
		char[] c=getCharArray(mFrom,mTo,symTab);
		int i=symTab.dsTraitPaths[ord].lookup(c);
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
		char[] c=new char[p.fromToLCA.length*2+p.toToLCA.length*2-3];
		int i=0;
		int[] ta=tte.getArr(to.span.s);
		for(int j=0,m=p.toToLCA.length-1;j<m;++j){
			c[i++]=(char) ta[p.toToLCA[j].getHead()];
			c[i++]='U';
		}
		int[] fa=tte.getArr(from.span.s);
		int r=c.length-1;
		for(int j=0,m=p.fromToLCA.length-1;j<m;++j){
			c[r--]=(char) fa[p.fromToLCA[j].getHead()];
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
