package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.IMType;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.ThreadPoolSingleton;

public class F_MType<T extends Enum<T> & IMType<T>> extends AbstractPairEnumFeature<T> implements ISingleMentionFeature {
	private static final long serialVersionUID = -6236330308376418903L;

	private final PairTargetNodeExtractor nodeExtractor;
	
	private final int mtIdx; 
	
	protected F_MType(String name, T[] ts,PairTargetNodeExtractor spanExtractor) {
		super(name, ts);
		this.mtIdx=IMType.mtIdxMap.indexOf(ts[0].getClass());
		this.nodeExtractor=spanExtractor;
	}
	
	private T computeT(INode node){
		return _ts[0].getType(node);
	}

	@Override
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int nIdx=nodeExtractor.getNodeInstIdxPrecompArray(tnes);
		return getIntValue(inst,nIdx);
	}

	@Override
	public int getIntValue(Instance inst, int nIdx) {
		if(nIdx<0)
			return -1;
		else
			return inst.mentionTypes[mtIdx][nIdx];
	}
	
	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.mentionTypes==null)
			inst.mentionTypes=new byte[IMType.mtIdxMap.size()][];
		if(inst.mentionTypes[mtIdx]!=null)
			return;
		inst.mentionTypes[mtIdx]=new byte[inst.nodes.length];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0;i<inst.nodes.length;++i)
					inst.mentionTypes[mtIdx][i]=(byte) computeT(inst.nodes[i]).ordinal();
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}
	
	@Override
	public boolean firstOrderFeature() {
		return nodeExtractor.firstOrder();
	}

}
