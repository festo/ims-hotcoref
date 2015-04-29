package ims.hotcoref.features;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
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

public class F_PronounForm extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = 2966797011852995683L;

	private final PairTargetNodeExtractor tse;
	
	protected F_PronounForm(PairTargetNodeExtractor tse) {
		super(tse.ts.toString()+"PronounForm");
		this.tse=tse;
	}

	@Override
	public int getBits(SymbolTable symTab) {
		return symTab.forms.getBits();
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.Form);
	}

	@Override
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int idx=tse.getNodeInstIdxPrecompArray(tnes);
		if(idx<0)
			return -1;
		else
			return inst.pronounForm[idx];
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.pronounForm!=null)
			return;
		inst.pronounForm=new int[inst.nodes.length];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0;i<inst.nodes.length;++i){
					final int v;
					if(inst.nodes[i] instanceof VNode){
						v=AbstractSymbolMapping.getVNodeIntVal((VNode) inst.nodes[i]);
					} else {
						Span sp=((MNode) inst.nodes[i]).span;
						if(sp.isPronoun)
							v=symTab.forms.lookup(sp.s.forms[sp.start]);
						else
							v=AbstractSymbolMapping.NONE_IDX;
					}
					inst.pronounForm[i]=v;
				}

				return null;
			}			
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	@Override
	public boolean firstOrderFeature() {
		return tse.firstOrder();
	}
}
