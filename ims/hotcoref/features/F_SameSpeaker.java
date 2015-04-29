package ims.hotcoref.features;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.TrueFalse;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.ThreadPoolSingleton;

public class F_SameSpeaker extends AbstractPairEnumFeature<TrueFalse>{
	private static final long serialVersionUID = 6799935618765985079L;

	private final PairTargetNodeExtractor tse1;
	private final PairTargetNodeExtractor tse2;
	
	protected F_SameSpeaker(PairTargetNodeExtractor t1,PairTargetNodeExtractor t2) {
		super(t1.ts.toString()+t2.ts.toString()+"SameSpeaker", TrueFalse.values());
		this.tse1=t1;
		this.tse2=t2;
	}

	protected TrueFalse computeT(int nF,int nT,Instance inst) {
		INode from=inst.nodes[nF];
		INode to=inst.nodes[nT];
		if(from instanceof VNode || to instanceof VNode)
			return TrueFalse.VNode;
		Span f=((MNode) from).span;
		Span t=((MNode) to).span;
		boolean sameSpeaker=f.s.speaker[f.start].equals(t.s.speaker[t.start]);
//		boolean undef=sameSpeaker && f.s.speaker[f.start].equals("-");
//		boolean undef=inst.sGenre.equals("pt") || inst.sGenre.equals("wb");
//		if(undef)
//			return null;
		if(sameSpeaker)
			return TrueFalse.True;
		else
			return TrueFalse.False;
	}

	@Override
	int getIntValue(Instance inst,int[] tnes,HOTState hotState) {
		int t=tse2.getNodeInstIdxPrecompArray(tnes);
		int s=tse1.getNodeInstIdxPrecompArray(tnes);
		int r=Math.max(s, t);
		int l=Math.min(t, s);
		if(l<0)
			return TrueFalse.None.ordinal();
		else
			return inst.sameSpeaker[r][l];
	}

	@Override
	public void XfillFillInstanceJobs(final Instance inst,final SymbolTable symTab, List<Callable<Void>> l, List<Future<Void>> fjs){
		if(inst.sameSpeaker!=null)
			return;
		inst.sameSpeaker=new byte[inst.nodes.length][];
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int nT=1;nT<inst.nodes.length;++nT){
					inst.sameSpeaker[nT]=new byte[nT];
					for(int nF=0;nF<nT;++nF){
						TrueFalse tf=computeT(nF,nT,inst);
//						inst.sameSpeaker[nT][nF]=tf==null?5:(byte)tf.ordinal(); 
						inst.sameSpeaker[nT][nF]=(byte)tf.ordinal();
					}
				}
				return null;
			}
		};
		fjs.add(ThreadPoolSingleton.getInstance().submit(j));
	}
	
	@Override
	public boolean firstOrderFeature() {
		return tse1.firstOrder() && tse2.firstOrder();
	}
}
