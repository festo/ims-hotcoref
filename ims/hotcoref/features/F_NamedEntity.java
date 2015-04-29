package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.NE;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.*;
import ims.hotcoref.symbols.AbstractSymbolMapping;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;
import ims.util.ThreadPoolSingleton;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class F_NamedEntity extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = -6104798447862613507L;

	private final PairTargetNodeExtractor nodeExtractor;
	
	protected F_NamedEntity(PairTargetNodeExtractor nodeExtractor) {
		super(nodeExtractor.ts.toString()+"NamedEntity");
		this.nodeExtractor=nodeExtractor;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.nes.getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.NE);
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst, final SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		if(inst.namedEntity!=null)
			return;
		inst.namedEntity=new byte[inst.nodes.length];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0;i<inst.nodes.length;++i){
					INode node=inst.nodes[i];
					final byte b;
					if(node.isVirtualNode())
						b=(byte) AbstractSymbolMapping.getVNodeIntVal((VNode) node);
					else{
						NE ne=((MNode) node).span.ne;
						b=(byte) (ne==null?AbstractSymbolMapping.NONE_IDX:symTab.nes.lookup(ne.getLabel()));
					}
					inst.namedEntity[i]=b;
				}
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	@Override
//	int getIntValue(Instance inst,int headIdx,int depIdx,HOTState hotState) {
//		int n=nodeExtractor.getNodeInstIdx(headIdx,depIdx,hotState);
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int n=nodeExtractor.getNodeInstIdxPrecompArray(tnes);
		return inst.namedEntity[n];
	}

	@Override
	public boolean firstOrderFeature() {
		return nodeExtractor.firstOrder();
	}
}
