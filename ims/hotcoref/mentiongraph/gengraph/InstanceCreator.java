package ims.hotcoref.mentiongraph.gengraph;

import gnu.trove.map.hash.TObjectIntHashMap;
import ims.hotcoref.Options;
import ims.hotcoref.data.Chain;
import ims.hotcoref.data.CorefSolution;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Span;
import ims.hotcoref.markables.GoldStandardChainExtractor;
import ims.hotcoref.markables.IMarkableExtractor;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.mentiongraph.NodeFactory;
import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.mentiongraph.VNode.VNodeType;
import ims.util.IntPair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstanceCreator implements Serializable {
	private static final long serialVersionUID = -7498639090364754745L;

	public static final String ZIP_ENTRY = "_ic";

	public final IMarkableExtractor markableExtractor;	
	
	public InstanceCreator(IMarkableExtractor markableExtractor){
		this.markableExtractor=markableExtractor;
	}

	
	public static long TIME_CREATE_EDGES=0;
	public static long TIME_SUBGRAPHS=0;
	public static long TIME_INSTANCE_CONSTRUCTOR=0;
	
	public int totalPrunedChains=0;
	public int totalPrunedMentions=0;
	public int totalInjectedGoldMentions=0;
	public int totalPrunedSingletonChains=0;
	
	public Instance createTrainingInstance(Document d){
		CorefSolution gold=GoldStandardChainExtractor.getGoldCorefSolution(d);
		if(!Options.dontPruneVerbs){
			IntPair ip=gold.pruneVerbs();
			totalPrunedChains+=ip.i1;
			totalPrunedMentions+=ip.i2;
		}
		totalPrunedSingletonChains+=gold.pruneSingletons();
		List<Span> spanList=getSortedSpanList(d,gold,markableExtractor);
		List<VNode> vNodes=getVNodes();
		INode[] allNodes=new INode[vNodes.size()+spanList.size()];
		List<MNode> headlessMNodes=new ArrayList<MNode>();
		for(int i=0;i<vNodes.size();++i)
			allNodes[i]=vNodes.get(i);
		Map<Span,Integer> gspan2IntMap=gold.getSpan2IntMap();
		for(int i=vNodes.size(),j=0;i<allNodes.length;++i,++j){
			Span sp=spanList.get(j);
			allNodes[i]=sp.getGraphNode();
			if(gspan2IntMap.get(sp)==null)
				headlessMNodes.add(sp.getGraphNode());
		}
		
//		long t0=System.currentTimeMillis();
//		TIME_CREATE_EDGES+=System.currentTimeMillis()-t0;
		long t1=System.currentTimeMillis();
		int[][] subgraphNodes=getSubgraphNodes(vNodes,headlessMNodes,gold,allNodes);
		TIME_SUBGRAPHS+=System.currentTimeMillis()-t1;
//		long t2=System.currentTimeMillis();
		Instance instance=new Instance(subgraphNodes,allNodes,d.genre);
//		TIME_INSTANCE_CONSTRUCTOR+=System.currentTimeMillis()-t2;
		return instance;
	}
	
	private int[][] getSubgraphNodes(List<VNode> vNodes,List<MNode> headlessMNodes, CorefSolution gold, INode[] allNodes) {
		//make a hash INode -> idx
		TObjectIntHashMap<INode> node2idx=new TObjectIntHashMap<INode>();
		for(int i=0;i<allNodes.length;++i)
			node2idx.put(allNodes[i], i);
		int[][] out=new int[headlessMNodes.size()+gold.getChainCount()][];
		int n=0;
		for(MNode m:headlessMNodes)
			out[n++]=new int[]{node2idx.get(m)};
		for(Chain c:gold.getKey()){
			out[n]=new int[c.spans.size()];
			int p=0;
			for(Span s:c.spans)
				out[n][p++]=node2idx.get(s.getGraphNode());
			++n;
		}
		return out;
	}

	public Instance createTestInstance(Document d){
		List<Span> spanList=getSortedSpanList(d,null,markableExtractor);
		List<VNode> vNodes=getVNodes();
		INode[] allNodes=new INode[vNodes.size()+spanList.size()];
		for(int i=0;i<vNodes.size();++i)
			allNodes[i]=vNodes.get(i);
		for(int i=vNodes.size(),j=0;i<allNodes.length;++i,++j)
			allNodes[i]=spanList.get(j).getGraphNode();
		Instance inst=new Instance(null, allNodes,d.genre);
		return inst;
	}
	
	public List<Span> getSortedSpanList(Document d,CorefSolution gold,IMarkableExtractor markableExtractor){
		Set<Span> spans=markableExtractor.extractMarkables(d);
		if(gold!=null && !Options.dontInjectGold){
			for(Span s:gold.getSpanSet())
				if(spans.add(s))
					totalInjectedGoldMentions++;
		}
		List<Span> spanList=new ArrayList<Span>(spans);
		Collections.sort(spanList);
		return spanList;
	}
	
	private static List<VNode> getVNodes(){
		return Arrays.asList(NodeFactory.getVirtualNode(VNodeType.GenericDocRoot));
	}

}
