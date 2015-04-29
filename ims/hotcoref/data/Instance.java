package ims.hotcoref.data;

import java.util.Arrays;

import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;

public class Instance {

	//Full graph
	public final INode[] nodes;                //The nodes
	public final int[][] chainNodes;           //The indices of the nodes in every chain
	public final int[]   nodeToChainNodeArr;
	public int[]         preHeads=null;

	public int[] lastIterHeads;
	
	public boolean filled=false;
	public Instance(int[][] chainNodes,INode[] nodes,String genre) {
		this.nodes = nodes;
		this.chainNodes=chainNodes;
		if(chainNodes==null) {
			nodeToChainNodeArr=null;
		} else {
			nodeToChainNodeArr=new int[nodes.length];
			nodeToChainNodeArr[0]=-2;
			for(int q=0;q<chainNodes.length;++q)
				for(int a:chainNodes[q])
					nodeToChainNodeArr[a]=q;
			this.lastIterHeads=new int[nodes.length];
			Arrays.fill(lastIterHeads, -1);
		}
		this.sGenre=genre;
	}

	
	public int genre=-1;
	public final String sGenre;
	
	//Node features:
	public int[][][]  tokenTraits;
//	public int[][][][] bagOfTraits;
	public int[]    pronounForm;
	public int[]    dominatingVerb;
	public int[][]  wsTokenTraits;
	public int[][]  cfgNodeCat;
	public int[][]  cfgNodeSubCat;
	public byte[]   anaphoricityBucketed;
	public byte[]   namedEntity;
	public byte[]   quoted;
	public byte[][] mentionTypes;
	public byte[] 	gender;
	public byte[]   number;
	public int[][]  bagOfCoordinations;
	public int[]    pathToRoot;
	public int[]    pathToRootWD;
	
	//Edge features::: the last two [][] are always [nF][nT] -- where nF>nT
	public byte[][]   cleverStringMatch;
	public byte[][]   exactStringMatch;
	public byte[][]   alias;
	public byte[][]   nested;
	public byte[][]   sameSpeaker;
	public byte[][][] sentDist;
	public byte[][][] headSubStrMatch;
//	public byte[][]   headSubStrMatch;
	public byte[][][][] traitWholeSpanEditDistance;
	public byte[][][][] tokenTraitWholeSpanEditDistance;	
	public int[][]    cfgSSPath;
	public int[][]    cfgDSPath;
	public int[][][]  cfgSSTraitPath;
	public int[][][]  cfgDSTraitPath;
	public int[][][]  esTrait;
	public int[][][] 	esTTrait;
	
	public void clearSpans(){
		for(INode node:nodes)
			if(node instanceof MNode)
				((MNode) node).span=null;
	}
	
}
