package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.lang.Language;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;
import ims.util.ThreadPoolSingleton;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_CoordinationBOW extends AbstractMultiPairFeature {
	private static final long serialVersionUID = -4109344245208636240L;

	private final PairTargetNodeExtractor tse;
	
	protected F_CoordinationBOW(PairTargetNodeExtractor tse) {
		super(tse.ts.toString()+"CoordinationBOW");
		this.tse=tse;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.coordTokens.getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.CoordTokens);
		t.add(Types.Form);
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst, final SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.bagOfCoordinations!=null)
			return;
		inst.bagOfCoordinations=new int[inst.nodes.length][];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0;i<inst.nodes.length;++i)
					inst.bagOfCoordinations[i]=getBag(inst.nodes[i],symTab);
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	private int[] getBag(INode iNode,SymbolTable symTab) {
		if(iNode.isVirtualNode())
			return null;
		Span s=((MNode) iNode).span;
		int[] coordIdx=new int[s.size()];
		int p=0;
		//count them
		for(int r=s.start;r<=s.end;++r)
			if(Language.getLanguage().isCoordToken(s.s.forms[r]))
				coordIdx[p++]=r;
		if(p==0)
			return null;
		int[] l=new int[p];
		for(int i=0;i<p;++i)
			l[i]=symTab.coordTokens.lookup(s.s.sInst.f[coordIdx[i]]);
		return l;
	}

	@Override
//	long[] getLongArr(SymbolTable symTab,Instance inst,int headIdx,int depIdx,HOTState hotState) {
	long[] getLongArr(SymbolTable symTab,Instance inst,int[] tnes,HOTState hotState) {
//		int[] i=inst.bagOfCoordinations[tse.getNodeInstIdx(headIdx,depIdx,hotState)];
		int[] i=inst.bagOfCoordinations[tse.getNodeInstIdxPrecompArray(tnes)];
		if(i==null)
			return null;
		long[] l=new long[i.length];
		for(int q=0;q<l.length;++q)
			l[q]=i[q];
		return l;
	}

	@Override
	public boolean firstOrderFeature() {
		return tse.firstOrder();
	}
}
