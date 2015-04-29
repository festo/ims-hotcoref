package ims.hotcoref.features;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.features.extractors.SpanToken;
import ims.hotcoref.features.extractors.SpanTokenExtractor;
import ims.hotcoref.features.extractors.TokenTrait;
import ims.hotcoref.features.extractors.TokenTraitExtractor;
import ims.hotcoref.symbols.AbstractSymbolMapping;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;
import ims.util.ThreadPoolSingleton;

public class F_PairNodeTokenTraitFeature extends AbstractAtomicPairFeature implements ISingleMentionFeature {
	private static final long serialVersionUID = 3682100289691673628L;
	
	private final PairTargetNodeExtractor tse;
	private final SpanTokenExtractor ste;
	private final TokenTraitExtractor tte;

	private final int stOrd;
	private final int ttOrd;
	
	
	public F_PairNodeTokenTraitFeature(PairTargetNodeExtractor tse,SpanTokenExtractor ste,TokenTraitExtractor tte) {
		super(tse.ts.toString()+ste.st.toString()+tte.tt.toString());
		this.tse=tse;
		this.ste=ste;
		this.tte=tte;
		this.stOrd=ste.st.ordinal();
		this.ttOrd=tte.tt.ordinal();
	}

	@Override
	public int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int idx=tse.getNodeInstIdxPrecompArray(tnes);
		return getIntValue(inst,idx);
	}
	
	@Override
	public int getIntValue(Instance inst,int idx){
		if(idx<0)
			return AbstractSymbolMapping.NONE_IDX;
		else
			return inst.tokenTraits[ttOrd][stOrd][idx];
	}

	@Override
	public int getBits(SymbolTable symTab) {
		int bits=tte.getBits(symTab);
		return bits;
	}

	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(tte.tt.getType());
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.tokenTraits==null)
			inst.tokenTraits=new int[TokenTrait.values().length][SpanToken.values().length][];
		if(inst.tokenTraits[ttOrd][stOrd]!=null)
			return;
		inst.tokenTraits[ttOrd][stOrd]=new int[inst.nodes.length];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0;i<inst.nodes.length;++i)
					inst.tokenTraits[ttOrd][stOrd][i]=tte.computeIntValue(inst.nodes[i], ste, symTab);
				return null;
			}
			public String toString(){
				return getName();
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}

	@Override
	public boolean firstOrderFeature() {
		return tse.firstOrder();
	}

}
