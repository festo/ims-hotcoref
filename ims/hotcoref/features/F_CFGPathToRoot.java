package ims.hotcoref.features;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.symbols.AbstractSymbolMapping;
import ims.hotcoref.symbols.CharArraySymbolMapping;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;
import ims.util.ThreadPoolSingleton;

public class F_CFGPathToRoot extends AbstractAtomicPairFeature {
	private static final long serialVersionUID = -6261790465464241918L;
	
	private final boolean chop;
	private final boolean withDir;
	private final PairTargetNodeExtractor tse;
	
	protected F_CFGPathToRoot(PairTargetNodeExtractor tse,boolean withDir,boolean chop) {
		super(tse.ts.toString()+"CFGPathToRoot"+(chop?"Chopped":"")+(withDir?"WD":""));
		this.tse=tse;
		this.withDir=withDir;
		this.chop=chop;
	}
	
	@Override
	public void addSymbolTypes(Set<Types> t) {
		t.add(Types.Category);
		if(withDir)
			t.add(Types.PathToRootWDir);
		else
			t.add(Types.PathToRoot);
	}
	@Override
	public int getBits(SymbolTable symTab) {
		return withDir?symTab.pathToRootWDir.getBits():symTab.pathToRoot.getBits();
	}
	@Override
	public void XfillFillInstanceJobs(final Instance inst, final SymbolTable symTab,List<Callable<Void>> l, List<Future<Void>> fjs) {
		final int[] sink;
		final CharArraySymbolMapping casm;
		if(withDir){
			if(inst.pathToRootWD!=null)
				return;
			inst.pathToRootWD=new int[inst.nodes.length];
			sink=inst.pathToRootWD;
			casm=symTab.pathToRootWDir;
		} else {
			if(inst.pathToRoot!=null)
				return;
			inst.pathToRoot=new int[inst.nodes.length];
			sink=inst.pathToRoot;
			casm=symTab.pathToRoot;
		}
		
		Callable<Void> j=new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				for(int i=0;i<inst.nodes.length;++i){
					INode node=inst.nodes[i];
					if(node.isVirtualNode()){
						sink[i]=AbstractSymbolMapping.UNK_IDX;
						continue;
					}
					MNode mn=(MNode) node;
					char[] c=pathToRoot(mn.span,symTab,withDir,chop);
					sink[i]=casm.lookup(c);
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
	@Override
	int getIntValue(Instance inst, int[] tnes, HOTState hotState) {
		int ix=tse.getNodeInstIdxPrecompArray(tnes);
		if(ix<0)
			return AbstractSymbolMapping.NONE_IDX;
		return withDir?inst.pathToRootWD[ix]:inst.pathToRoot[ix];
	}
	
	
	static char[] pathToRoot(Span sp,SymbolTable symTab,boolean wDir,boolean chop){
		int cnt=1;
		CFGNode n=sp.cfgNode;
		int offset=0;
		if(n==null){
			n=sp.s.ct.getMinimalIncludingNode(sp.start, sp.end);
			++cnt;
			++offset;
		}
		final CFGNode start=n;
		CFGNode last=n;
		for(n=n.getParent();n!=null && !n.getLabel().equals("DUMMY");last=n,n=n.getParent()){
			cnt++;
			if(doChop(last,chop))
				break;
		}
		char[] r=new char[2*cnt-1];
		int cur=0;
		if(offset==1){
			r[cur++]=(char) AbstractSymbolMapping.NONE_IDX;
			r[cur++]=NORTH;
		}
		r[cur++]=(char) symTab.cats.lookup(n.getLabel());
		n=start;
		last=n;
		for(n=n.getParent();n!=null && !n.getLabel().equals("DUMMY");last=n,n=n.getParent()){
			final char c;
			if(wDir){
				int lHead=last.getHead();
				int curHead=n.getHead();
				if(lHead==curHead)
					c=NORTH;
				else if(lHead<curHead)
					c=NORTH_EAST;
				else
					c=NORTH_WEST;
			} else {
				c=NORTH;
			}
			r[cur++]=c;
			r[cur++]=(char) symTab.cats.lookup(n.getLabel());
			if(doChop(last,chop))
				break;
		}
		if(cur!=r.length)
			throw new Error("!");
		return r;
	}

	private static boolean doChop(CFGNode last, boolean chop) {
		if(!chop)
			return false;
		String lbl=last.getLabel();
		return lbl.equals("S") || lbl.equals("IP") || lbl.equals("CP");
	}

	private static final char NORTH=(char) 5;
	private static final char NORTH_WEST=(char) 6;
	private static final char NORTH_EAST=(char) 7;

}
