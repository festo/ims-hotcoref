package ims.hotcoref.decoder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import ims.hotcoref.LearnWeights;
import ims.hotcoref.data.CorefSolution;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Instance;
import ims.hotcoref.features.FeatureSet;
import ims.hotcoref.mentiongraph.Edge;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.perceptron.FV3;
import ims.hotcoref.perceptron.Long2IntInterface;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.Pair;
import ims.util.ThreadPoolSingleton;

public class Decoder implements Serializable {
	private static final long serialVersionUID = -6929898610506621L;

	public static final String ZIP_ENTRY = "_decoder";
	
	public final float[] parameters;
	public final Long2IntInterface l2i;
	public final FeatureSet fs;
	public final SymbolTable symTab;
	
//	public final int maxVioRightWindow;
//	public final int guidedCount;
	
	public Decoder(float[] parameters,Long2IntInterface l2i,FeatureSet fs,SymbolTable symTab){ //,HOTLR mstDecoder) {
		this.parameters=parameters;
		this.l2i=l2i;
		this.fs=fs;
		this.symTab=symTab;
//		this.maxVioRightWindow=maxVioRightWindow;
//		this.guidedCount=guidedCount;
	}
	
	public CorefSolution hotState2CS(HOTState hs,Instance inst){
		CorefSolution cs=new CorefSolution();
		int[] heads=hs.heads;
		for(int i=2;i<heads.length;++i){
			if(heads[i]!=0){
				MNode head=(MNode) inst.nodes[heads[i]];
				MNode dep =(MNode) inst.nodes[i];
				cs.addLink(head.span,dep.span);
			}
		}
		return cs;
	}
	
	public HOTState corefMST(Instance inst,int beam) throws InterruptedException, ExecutionException{
		final Edge[][] edges=scoreAllFOEdges(inst,false);
		final HOTState r;
		if(beam==1)
			r=predictMSTNoBeam(edges,inst,false,false);
		else
			r=predictMSTwithBeam(edges,inst,false,false,beam)[0];
		return r;
	}
	
	
	public HOTState getLatentPredictionMST(Document d,Instance inst,int beam) {
		fs.fillInstance(inst, d, symTab);
		Edge[][] edges=scoreAllFOEdges(inst,false);
		try {
			HOTState hs=getLatentMST(edges, inst, false, false,null,beam);
			return hs;
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public HOTState[] predictMSTwithBeam(final Edge[][] edges,final Instance inst,final boolean threadSafe,final boolean skipHOT,final int beamSize) throws InterruptedException, ExecutionException{
		final int nodes=inst.nodes.length;
		HOTState[] agenda=new HOTState[]{new HOTState(nodes)};
		ThreadPoolSingleton tps=ThreadPoolSingleton.getInstance();
		for(int dep=1;dep<nodes;++dep){
			List<Future<Pair<BeamExpansion[],BeamExpansion>>> predFs=new ArrayList<Future<Pair<BeamExpansion[],BeamExpansion>>>();
			for(HOTState p:agenda)
				predFs.add(tps.submit(new BeamStateExpander2(edges[dep],inst,p,beamSize,false,false)));
			
			BeamExpansionMinHeap keep=new BeamExpansionMinHeap(beamSize);
			for(Future<Pair<BeamExpansion[],BeamExpansion>> f:predFs){
				BeamExpansion[] bes=f.get().getLeft();
				for(BeamExpansion be:bes)
					if(keep.offer(be.score, be.edgeLen))
						keep.insert(be);
					else
						break;
			}
			BeamExpansion[] selected=keep.emptyReverse();
			agenda=beamExpansionArrToHOTStateArr(selected,inst,true); //we say certainly gold here, obviously we don't know, but there's no point trying to track this.
		}
		return agenda;
	}
	
	public HOTState predictMSTNoBeam(final Edge[][] edges,final Instance inst,boolean threadSafe,boolean skipHOT){
		final int nodes=inst.nodes.length;
		HOTState hotState=new HOTState(nodes);
		for(int dep=1;dep<nodes;++dep){ //Find a head for 1, 2, 3, ...
			long t0=System.currentTimeMillis();
			HOTEdge[] hoes=getHOTEdges(fs,symTab,inst,hotState,edges[dep],threadSafe,skipHOT);
			long t1=System.currentTimeMillis();
			int maxIdx=-1;
			double maxScore=Float.NEGATIVE_INFINITY;
			for(int head=0;head<hoes.length;++head){
				if(hoes[head].score()>=maxScore){
					maxScore=hoes[head].score();
					maxIdx=head;
				}
			}
			HOTEdge best=hoes[maxIdx];
			hotState.appendEdge(best);
			long t2=System.currentTimeMillis();
			predTime+=t2-t1;
			hoTime+=t1-t0;
		}
		return hotState;
	}
	
	
	public long predTime=0;
//	public long latTime=0;
	public long hoTime=0;

	public void resetTime() {
		predTime=0;
//		latTime=0;
		hoTime=0;
//		maxVioEUDiff=0;
		scoreEdgeTime=0;
		latentTime=0;
		failBreaks=0;
		countBreaks=0;
		
//		beiEdgeScoreBeamGold=0;
//		beiEdgeScore=0;
//		beiSearchBeamGold=0;
//		beiSearchBeam=0;
//		beiAdvanceBeam=0;
//		beiExtractAgenda=0;
//		beiExtractAgendaGold=0;
//		beiSingleThreaded=0;
		beiPredictState=0;
		beiBeamGoldState=0;
	}

	public long scoreEdgeTime=0;
	public long latentTime=0;
	
	
	public int failBreaks=0;
	public int countBreaks=0;

//	public GuidedReturn guidedLearn(Instance inst, Regularizer regularizer, float rootLoss,ParametersFloat params,double upd,boolean precomputeHeads,int[] predefinedHeads) {
//		if(predefinedHeads==null && precomputeHeads){
//			Edge[][] edges=scoreEdges(inst,!l2i.frozen());
//			HOTState l=getLatentMST(edges,inst,!l2i.frozen(),false,null);
//			predefinedHeads=l.getHeads();
//		}
//		HOTState state=new HOTState(inst.nodes.length);
//		FV gold=new FV();
//		FV pred=new FV();
//		int updates=0;
//		float accLoss=0;
//		int fail=0;
//		for(int dep=1;dep<inst.nodes.length;++dep){
//			--upd;
//			long t0=System.currentTimeMillis();
//			HOTEdge[] hotEdges=getHOTEdges(dep,inst,state);
//			long t1=System.currentTimeMillis();
//			scoreEdgeTime+=t1-t0;
//			//(Latent) head:
//			final int headIdx;
//			{
//				if(predefinedHeads!=null){
//					headIdx=predefinedHeads[dep];
//				} else {
//					int[] nod=inst.chainNodes[inst.nodeToChainNodeArr[dep]];
//					if(nod[0]==dep){ //this is the first node in a chain, then it has to link to the root
//						//					g=hotEdges[0];
//						headIdx=0;
//					} else {
//						//otherwise we need to take the max over the edges that connect to the left (but not to root)
//						//					g=hotEdges[nod[0]];
//						int q=nod[0];
//						for(int hix=1;nod[hix]!=dep;++hix)
//							if(hotEdges[nod[hix]].score()>=hotEdges[q].score())
//								q=nod[hix];
//						headIdx=q;
//					}
//				}
//			}
//			HOTEdge g=hotEdges[headIdx];
//			long t2=System.currentTimeMillis();
//			latentTime+=t2-t1;
//			//Predicted head:
//			HOTEdge p=getMax(hotEdges,dep);
//			int c=0;
//			float loss;
////			while(p.getHeadIdx()!=g.getHeadIdx()){
//			while((loss=LearnWeights.getLoss(inst.nodeToChainNodeArr, g.getHeadIdx(), p.getHeadIdx(), rootLoss))>0.f){
//				c++;
//				//Update, rescore, and take argmax.
//				fillVector(gold,g);
//				fillVector(pred,p);
////				float loss=p.getHeadIdx()==0?rootLoss:1.0f;
//				accLoss+=loss;
//				if(!params.update(gold, pred, upd, loss, regularizer)){ //break if update fails
//					++fail;
//					++failBreaks;
//					break;
//				}
//				++updates;
//				long ta=System.currentTimeMillis();
//				hotEdges=getHOTEdges(dep,inst,state);
//				scoreEdgeTime+=System.currentTimeMillis()-ta;
//				p=getMax(hotEdges,dep);
//				g=hotEdges[headIdx];
//				if(c==guidedCount){
////					System.out.println("\nBREAKING");
////					++fail;
//					++countBreaks;
//					break;
//				}
//					
//			}
//			state.appendEdge(g);
//		}
//		return new GuidedReturn(updates,state,accLoss,fail);
//	}
	
	
//	class GuidedEdgeScorer implements Callable<Void>{
//		int start;
//		final int dep;
//		final int stop;
//		final long[] sink;
//		final HOTEdge[] r;
//		final Instance inst;
//		final HOTState state;
//		public GuidedEdgeScorer(int start, int stop, int dep,long[] sink, HOTEdge[] r,Instance inst,HOTState state) {
//			this.start = start;
//			this.stop = stop;
//			this.dep=dep;
//			this.sink = sink;
//			this.r = r;
//			this.inst=inst;
//			this.state=state;
//		}
//		@Override
//		public Void call() throws Exception {
//			for(int head=start;head<stop;++head)
//				r[head]=fs.createHOTEdge(l2i, parameters, symTab, inst, sink, state, fs.createScoredEdge(head, dep, l2i, parameters, symTab, inst, sink));
//			return null;
//		}
//	}
	
//	public HOTEdge[] getHOTEdges(int dep,Instance inst,HOTState hotState){
//		HOTEdge[] r=new HOTEdge[dep];
//		//single-threaded
////		long[] sink=new long[2000];
////		for(int head=0;head<dep;++head)
////			r[head]=fs.createHOTEdge(l2i, parameters, symTab, inst, sink, hotState, fs.createScoredEdge(head, dep, l2i, parameters, symTab, inst, sink));
////		int threads=ThreadPoolSingleton.threadCount()>3?3:ThreadPoolSingleton.threadCount();
//		int threads=12;
//		int inc=Math.max(10, dep/threads)+1;
//		
//		List<Future<Void>> f=new ArrayList<Future<Void>>();
//		ThreadPoolSingleton tps=ThreadPoolSingleton.getInstance();
//		for(int start=0;start<dep;start+=inc)
//			f.add(tps.submit(new GuidedEdgeScorer(start,Math.min(start+inc,dep),dep,new long[1500],r,inst,hotState)));
//		for(Future<Void> q:f)
//			try {
//				q.get();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		return r;
//	}
	
//	public static class GuidedReturn {
//		public final int updates;
//		public final HOTState state;
//		public final float accLoss;
//		public final int fail;
//		public GuidedReturn(int updates, HOTState state,float accLoss,int fail) {
//			this.updates = updates;
//			this.state = state;
//			this.accLoss=accLoss;
//			this.fail=fail;
//		}
//
//	}
	
	private BeamExpansion[] expandBeamState2(final Edge[] edges,final HOTState state,final Instance inst,final int dep,final boolean threadSafe,final boolean skipHOT,final int beamSize){
		final HOTEdge[] hotEdges=getHOTEdges(fs,symTab,inst,state,edges,threadSafe,skipHOT);
		BeamExpansion[] r=new BeamExpansion[hotEdges.length];
		for(int i=0,j=hotEdges.length-1;j>=0;++i,--j){
			BeamExpansion be=new BeamExpansion(state, hotEdges[j]);
			r[i]=be;
		}
		Arrays.sort(r,BeamExpansion.REV_BEAM_EXPANSION_SCORE_AND_EDGELEN_COMPARATOR);
		return r;
	}
	
//	private HOTState[] expandBeamState(final Edge[] edges,final HOTState state,final Instance inst,final int dep,final boolean threadSafe,final boolean skipHOT,final int beamSize,boolean extractAll){
//		//There are dep potential expansions, hence, if dep < beam, we have to expand them all and return them
//		final HOTEdge[] hotEdges=getHOTEdges(fs,symTab,inst,state,edges,threadSafe,skipHOT);
//		if(extractAll || hotEdges.length<beamSize){
//			HOTState[] r=new HOTState[hotEdges.length];
//			for(int i=0;i<hotEdges.length;++i){
//				HOTState hs=state.copy();
//				hs.appendEdge(hotEdges[i]);
//				r[i]=hs;
//			}
//			Arrays.sort(r,HOTState.REV_SCORE_AND_EDGELEN_COMPARATOR);
//			return r;
//		} else { // if there are more than beamSize edges to consider, we only copy the HOTState when we need to.
//			     // in order to avoid copying when we don't need to, we keep track of the worst score at the moment with a heap
////			MinHeap<HOTState> h=new MinHeap<HOTState>(new HOTState[beamSize]);
//			HOTStateMinHeap h=new HOTStateMinHeap(beamSize);
//			for(int i=0;i<hotEdges.length;++i){
//				HOTEdge he=hotEdges[i];
//				float score=state.score()+he.score();
//				int len=state.edgeLenSum+he.getLen();
//				if(h.offer(score,len)){
//					HOTState s=state.copy();
//					s.appendEdge(he);
//					h.insert(s);
//				}
//			}
//			HOTState[] r=h.emptyReverse();
//			return r;
//		}
//	}
	


	public HOTState getLatentMST(final Edge[][] edges,final Instance inst,boolean threadSafe,boolean skipHOT,int[] predefinedHeads,int beam) throws InterruptedException, ExecutionException{
		final int nodes=inst.nodes.length;
		if(beam==1){
			HOTState state=new HOTState(nodes);
			for(int dep=1;dep<nodes;++dep){
				int[] chainNodes=inst.chainNodes[inst.nodeToChainNodeArr[dep]];
				final Edge[] chainEdges;
				if(chainNodes[0]==dep){ //Has to go to the root.
					chainEdges=new Edge[]{edges[dep][0]};
				} else if(predefinedHeads!=null){
					chainEdges=new Edge[]{edges[dep][predefinedHeads[dep]]};
				} else { //Count how many preceding nodes there are in the chain, and extract those edges
					chainEdges=extractGoldEdges(edges,inst,dep);
				}
				HOTEdge[] hes=getHOTEdges(fs,symTab,inst,state,chainEdges,threadSafe,skipHOT);
				//Take the max
				HOTEdge best=hes[0];
				for(int r=1;r<hes.length;++r)
					if(hes[r].score()>=best.score())
						best=hes[r];
				state.appendEdge(best);
			}
			return state;
		} else {
			HOTState[] agenda=new HOTState[]{new HOTState(nodes)};
			for(int d=1;d<inst.nodes.length;++d){
				//Constrain the edges
				final Edge[] ed=extractGoldEdges(edges, inst, d);
				List<Future<Pair<BeamExpansion[], BeamExpansion>>> fs = issueGoldBeamExpansion(inst, beam, skipHOT, agenda, ed);
				agenda=extractAgendaFromBeamGoldExpansion2(beam,fs,inst);
			}
			return agenda[0];
		}
	}

	private Edge[] extractGoldEdges(final Edge[][] edges,final Instance inst,int d) {
		final Edge[] ed;
		int[] chainNodes=inst.chainNodes[inst.nodeToChainNodeArr[d]];
		if(chainNodes[0]==d)
			ed=new Edge[]{edges[d][0]};
		else {
			int cnt=1;
			for(int p=1;chainNodes[p]!=d;++p)
				++cnt;
			ed=new Edge[cnt];
			for(int p=0;chainNodes[p]!=d;++p)
				ed[p]=edges[d][chainNodes[p]];
		}
		return ed;
	}
	
	class GoldFOEdges {
		Future<Edge[]> f;
		long[] longs=new long[2000];
		public GoldFOEdges(final Instance inst,final int dep){
			Callable<Edge[]> c=new Callable<Edge[]>(){
				@Override
				public Edge[] call() throws Exception {
					int[] chainNodes=inst.chainNodes[inst.nodeToChainNodeArr[dep]];
					if(chainNodes[0]==dep){ //Has to go to the root.
						return new Edge[]{fs.createScoredEdge(0, dep, l2i, parameters, symTab, inst, longs)};
					}
					int cnt=1;
					for(int p=1;chainNodes[p]!=dep;++p)
						++cnt;
					Edge[] es=new Edge[cnt];
					for(int p=0;p<cnt;++p)
						es[p]=fs.createScoredEdge(chainNodes[p], dep, l2i, parameters, symTab, inst, longs);
					return es;
				}
			};
			f=ThreadPoolSingleton.getInstance().submit(c);
		}
		
		public Edge[] getNext() throws InterruptedException, ExecutionException{
			return f.get();
		}
	}
	
	private HOTState[] goldBeamFromStart(final Instance inst, final int beam,int depStart,boolean skipHOT) throws InterruptedException, ExecutionException {
		if(depStart==1)
			return new HOTState[]{new HOTState(inst.nodes.length)};

		HOTState[] agenda=new HOTState[]{new HOTState(inst.nodes.length)};
		GoldFOEdges gfoe=new GoldFOEdges(inst,1);
		for(int d=1;d<depStart;++d){
			final Edge[] es=gfoe.getNext();
			List<Future<Pair<BeamExpansion[], BeamExpansion>>> fs = issueGoldBeamExpansion(inst, beam, skipHOT, agenda, es);
			if(d+1<depStart)
				gfoe=new GoldFOEdges(inst,d+1);
			agenda=extractAgendaFromBeamGoldExpansion2(beam,fs,inst);
		}
		return agenda;
	}

	private List<Future<Pair<BeamExpansion[], BeamExpansion>>> issueGoldBeamExpansion(final Instance inst, final int beam, boolean skipHOT,HOTState[] agenda, final Edge[] es) {
		ThreadPoolSingleton tps=ThreadPoolSingleton.getInstance();
		List<Future<Pair<BeamExpansion[],BeamExpansion>>> fs=new ArrayList<Future<Pair<BeamExpansion[],BeamExpansion>>>();
		for(final HOTState h:agenda){
			Callable<Pair<BeamExpansion[],BeamExpansion>> c=new BeamStateExpander2(es,inst,h,beam,skipHOT,false);
			fs.add(tps.submit(c));
		}
		return fs;
	}
	
	class BeamStateExpander2 implements Callable<Pair<BeamExpansion[],BeamExpansion>>{
		final Edge[] edges;
		final Instance inst;
		final HOTState state;
		final int beam;
		final boolean skipHOT;
		final boolean trackGold;
		public BeamStateExpander2(Edge[] edges, Instance inst, HOTState state,int beam,boolean skipHOT,boolean trackGold) {
			this.edges = edges;
			this.inst = inst;
			this.state = state;
			this.beam = beam;
			this.skipHOT = skipHOT;
			this.trackGold = trackGold;
		}
		@Override
		public Pair<BeamExpansion[],BeamExpansion> call() throws Exception {
			final int dep=edges[0].depIdx;
			BeamExpansion[] hss=expandBeamState2(edges,state,inst,dep,!l2i.frozen(),skipHOT,beam);
			BeamExpansion bestCorrect=null;
			if(trackGold && state.getCorrect()){ //Have to check if this is correct
				for(BeamExpansion be:hss){
					boolean correct=sameChain(dep,be.ed.e.headIdx,inst);
					if(correct){ //Track best correct
						if(bestCorrect==null){
							bestCorrect=be;
						} else {
							if((be.score>bestCorrect.score) || //better score
									(be.score==bestCorrect.score && be.edgeLen<bestCorrect.edgeLen)) //same score, but shorter dist
								bestCorrect=be;
						}
					}
				}
			}
			return new Pair<BeamExpansion[],BeamExpansion>(hss,bestCorrect);
		}
	}

//	public Pair<List<UpdStruct>,HOTState> advanceIterativeEarlyBeamStateDU(final Instance inst,final int beam,boolean skipHOT,boolean threadSafe,float rootLoss,double upd) throws InterruptedException, ExecutionException{
	public BeamDUReturn advanceIterativeEarlyBeamStateDU(final Instance inst,final int beam,boolean skipHOT,boolean threadSafe,float rootLoss,double upd) throws InterruptedException, ExecutionException{
		Edge[][] foEdges=scoreAllFOEdges(inst,!l2i.frozen());
		HOTState[] goldAgenda=new HOTState[]{new HOTState(inst.nodes.length)};
		HOTState[] predAgenda=new HOTState[]{new HOTState(inst.nodes.length)};
		List<BeamExpansion> goldCands=new ArrayList<BeamExpansion>();
		FV3 goldFV=new FV3();
		FV3 predFV=new FV3();
//		List<UpdStruct> uss=new ArrayList<UpdStruct>();
		float accLoss=0.f;
		int ups=0;
		for(int dep=1;dep<inst.nodes.length;++dep){
			//Advance gold
			long t0=System.currentTimeMillis();
			
			Edge[] goldEdges=extractGoldEdges(foEdges, inst, dep);
			List<Future<Pair<BeamExpansion[], BeamExpansion>>> fs = issueGoldBeamExpansion(inst, beam, skipHOT, goldAgenda, goldEdges);
			goldAgenda=extractAgendaFromBeamGoldExpansion2(beam,fs,inst);

			long t1=System.currentTimeMillis();
			
			//Advance prediction
			List<Future<Pair<BeamExpansion[], BeamExpansion>>> predFs = issuePredExpansions(inst, beam, skipHOT, predAgenda, foEdges[dep]);
			predAgenda=extractPredictionAgenda(inst,beam,goldCands,predFs);
			boolean goldInPrediction=containsGold(predAgenda);
			
			long t2=System.currentTimeMillis();

			if(!goldInPrediction){ //store update
				HOTState gold=getBestGold2(goldCands).toStateCertainlyGold();
				HOTState pred=predAgenda[0];
				fillVector(predFV,pred.getHOTEdges());
				fillVector(goldFV,gold.getHOTEdges());
				ups++;
				float loss=LearnWeights.getLoss5(gold.getHOTEdges(), pred.getHOTEdges(), rootLoss, inst);
				accLoss+=loss;
				predAgenda=Arrays.copyOf(goldAgenda, goldAgenda.length);
			}
			beiPredictState+=t2-t1;
			beiBeamGoldState+=t1-t0;
		}
		HOTState pred=predAgenda[0];
		if(!pred.getCorrect() && containsGold(predAgenda)){
			HOTState gold=getBestGold2(goldCands).toStateCertainlyGold();
			float loss=LearnWeights.getLoss5(gold.getHOTEdges(), pred.getHOTEdges(), rootLoss, inst);
			accLoss+=loss;
			ups++;
			fillVector(predFV,pred.getHOTEdges());
			fillVector(goldFV,gold.getHOTEdges());
			return new BeamDUReturn(goldFV,predFV,gold,ups,accLoss);
		} else {
			HOTState gold=goldAgenda[0];
			return new BeamDUReturn(goldFV,predFV,gold,ups,accLoss);
		}
	}
	
	public static class BeamDUReturn {
		public final FV3 goldFV;
		public final FV3 predFV;
		public final HOTState gold;
		public final int ups;
		public final float loss;
		public BeamDUReturn(FV3 goldFV, FV3 predFV, HOTState gold, int ups,float loss) {
			this.goldFV = goldFV;
			this.predFV = predFV;
			this.gold = gold;
			this.ups = ups;
			this.loss=loss;
		}
	}
	
	static void fillVector(FV3 fv,List<HOTEdge> edges){
		for(HOTEdge e:edges){
			fv.add(e.getFO());
			fv.add(e.getHO());
		}
	}
	
	public long beiBeamGoldState=0;
	public long beiPredictState=0;
	public Pair<HOTState,HOTState> getIterativeEarlyBeamState2(final Instance inst,final int beam,int depStart,boolean skipHOT) throws InterruptedException, ExecutionException{
		//Set up init states
		long ta0=System.currentTimeMillis();
		HOTState[] agenda=goldBeamFromStart(inst,beam,depStart,skipHOT);
		long ta1=System.currentTimeMillis();
		beiBeamGoldState+=ta1-ta0;
		//Now expand both agendas in parallel
		List<BeamExpansion> goldCands=new ArrayList<BeamExpansion>();
		EdgesFuture efs=scoreEdgesByDep(inst,!l2i.frozen(),depStart);
		for(int d=depStart;d<inst.nodes.length;++d){
			//All edges
			final Edge[] es=efs.get();
			
			//Now expand
			List<Future<Pair<BeamExpansion[], BeamExpansion>>> predFs = issuePredExpansions(inst, beam, skipHOT, agenda, es);
			//Start scoring next set of edges
			if(d+1<inst.nodes.length)
				efs=scoreEdgesByDep(inst,!l2i.frozen(),d+1);
			//Extract gold and new agenda
			agenda = extractPredictionAgenda(inst, beam, goldCands, predFs);
			//Does the beam contain a correct item?
			boolean hasGold = containsGold(agenda);
			if(!hasGold){ //need to make update
				HOTState gold=getBestGold2(goldCands).toStateCertainlyGold();
				beiPredictState+=System.currentTimeMillis()-ta1;
				return new Pair<HOTState,HOTState>(gold,agenda[0]);
			}
		}
		final HOTState gold;
		if(agenda[0].getCorrect())
			gold=null;
		else
			gold=getBestGold2(goldCands).toStateCertainlyGold();
		beiPredictState+=System.currentTimeMillis()-ta1;
		return new Pair<HOTState,HOTState>(gold,agenda[0]);
	}

	private boolean containsGold(HOTState[] agenda) {
		for(HOTState hs:agenda){
			if(hs.getCorrect()){
				return true;
			}
		}
		return false;
	}

	private HOTState[] extractPredictionAgenda(final Instance inst,final int beam, List<BeamExpansion> goldCands,List<Future<Pair<BeamExpansion[], BeamExpansion>>> predFs)	throws InterruptedException, ExecutionException {
		HOTState[] agenda;
		BeamExpansionMinHeap heap=new BeamExpansionMinHeap(beam);
		goldCands.clear();
		for(Future<Pair<BeamExpansion[],BeamExpansion>> f:predFs){
			Pair<BeamExpansion[],BeamExpansion> p=f.get();
			BeamExpansion gold=p.getRight();
			if(gold!=null)
				goldCands.add(gold);
			BeamExpansion[] hss=p.getLeft();
			for(BeamExpansion hs:hss){
				if(heap.offer(hs.score,hs.edgeLen)){
					heap.insert(hs);
				} else {
					break;
				}
			}
		}
		//Now see if one best in gold agenda is anywhere in the pred agenda, otherwise halt.
		agenda=beamExpansionArrToHOTStateArr(heap.emptyReverse(),inst,false);
		return agenda;
	}

	private List<Future<Pair<BeamExpansion[], BeamExpansion>>> issuePredExpansions(final Instance inst, final int beam, boolean skipHOT,HOTState[] agenda, final Edge[] es) {
		ThreadPoolSingleton tps=ThreadPoolSingleton.getInstance();
		List<Future<Pair<BeamExpansion[],BeamExpansion>>> predFs=new ArrayList<Future<Pair<BeamExpansion[],BeamExpansion>>>();
		for(HOTState p:agenda)
			predFs.add(tps.submit(new BeamStateExpander2(es,inst,p,beam,skipHOT,true)));
		return predFs;
	}

	
	public static boolean sameChain(int dep, int head, Instance inst) {
		if(head==0){ //If the head is root, we have to check that dep is the first in its cluster
			return inst.chainNodes[inst.nodeToChainNodeArr[dep]][0]==dep;
		} else { //Else they have to map to the same
			return inst.nodeToChainNodeArr[dep]==inst.nodeToChainNodeArr[head];
		}
	}

	private BeamExpansion getBestGold2(List<BeamExpansion> goldCands) {
		if(goldCands.isEmpty())
			throw new Error("!");
		if(goldCands.size()==1)
			return goldCands.get(0);
		
		BeamExpansion bestGold=goldCands.get(0);
		int bestGoldDistSum=bestGold.edgeLen;
		for(int i=1;i<goldCands.size();++i){
			BeamExpansion hs=goldCands.get(i);
			if(hs.score<bestGold.score)
				continue;
			int q=hs.edgeLen;
			if(hs.score>bestGold.score || //if hs has higher score
					q<bestGoldDistSum){       //or if hs has equal score, but lower sum of distances
				bestGold=hs;
				bestGoldDistSum=q;
			}
		}
		return bestGold;
	}
	
	private HOTState[] extractAgendaFromBeamGoldExpansion2(final int beam,List<Future<Pair<BeamExpansion[],BeamExpansion>>> goldFs, Instance inst) throws InterruptedException,ExecutionException {
		BeamExpansionMinHeap heap=new BeamExpansionMinHeap(beam);
		for(Future<Pair<BeamExpansion[],BeamExpansion>> f:goldFs){
			BeamExpansion[] hs=f.get().getLeft();
			for(BeamExpansion h:hs){
				if(heap.offer(h.score,h.edgeLen))
					heap.insert(h);
				else
					break;
			}
		}
		BeamExpansion[] bes=heap.emptyReverse();
		
		HOTState[] newAgenda=beamExpansionArrToHOTStateArr(bes,inst,true);
		return newAgenda;
	}
	
	private HOTState[] beamExpansionArrToHOTStateArr(final BeamExpansion[] bes,final Instance inst,final boolean certainlyGold) throws InterruptedException, ExecutionException{
		final HOTState[] r=new HOTState[bes.length];
		List<Future<Void>> fjs=new ArrayList<Future<Void>>();
		ThreadPoolSingleton tps=ThreadPoolSingleton.getInstance();
		for(int i=0;i<bes.length;i+=5){
			final int q=i;
			Callable<Void> c=new Callable<Void>(){
				@Override
				public Void call() throws Exception {
					int to=Math.min(q+5, r.length);
					for(int p=q;p<to;++p)
						r[p]=bes[p].toState(inst,certainlyGold);
					return null;
				}
			};
			fjs.add(tps.submit(c));
		}
		for(Future<Void> f:fjs)
			f.get();
		return r;
	}
	
	static class EdgesFuture {
		private final Edge[] r;
		private final List<Future<Void>> fs;
		EdgesFuture(Edge[] r,List<Future<Void>> fs){
			this.r=r;
			this.fs=fs;
		}
		Edge[] get() throws InterruptedException, ExecutionException{
			for(Future<Void> f:fs)
				f.get();
			return r;
		}
	}
	
	EdgesFuture scoreEdgesByDep(Instance inst,boolean threadSafe,int dep) throws InterruptedException, ExecutionException{
		final Edge[] r=new Edge[dep];
		int inc=Math.max(8, dep/12+1);
		List<Future<Void>> fjs=new ArrayList<Future<Void>>();
		ThreadPoolSingleton tps=ThreadPoolSingleton.getInstance();
		for(int s=0;s<dep;s+=inc)
			fjs.add(tps.submit(threadSafe?new ThreadSafeSingleDepEdgeScorer(r, inst, s, Math.min(s+inc, dep), dep,new long[2000]):new SingleDepEdgeScorer(r, inst, s, Math.min(s+inc, dep), dep,new long[2000])));
		return new EdgesFuture(r,fjs);
	}
	
	public Edge[][] scoreAllFOEdges(Instance inst,boolean threadSafe){
		final Edge[][] r=new Edge[inst.nodes.length][];
		int edges=0;
		for(int i=1;i<inst.nodes.length;++i){
			r[i]=new Edge[i];
			edges+=i;
		}
		int threads=ThreadPoolSingleton.threadCount();
		if(edges<48)
			threads=1;
		int increment=edges/threads;
		List<Callable<Void>> edgeScorers=new ArrayList<Callable<Void>>();
		int nextBegin=1;
		for(int i=0;i<threads-1;++i){
			int end=nextBegin+increment;
			AllEdgeScorer es=threadSafe?new ThreadSafeAllEdgeScorer(r,inst,nextBegin,end):new AllEdgeScorer(r,inst,nextBegin,end);
			edgeScorers.add(es);
			nextBegin=end;
		}
		int max=edges+1;
		AllEdgeScorer last=threadSafe?new ThreadSafeAllEdgeScorer(r,inst,nextBegin,max):new AllEdgeScorer(r,inst,nextBegin,max);
		edgeScorers.add(last);
		List<Future<Void>> fs=ThreadPoolSingleton.getInstance().invokeAll(edgeScorers);
		try{
			for(Future<Void> f:fs)
				f.get();
		} catch(Exception e){
			throw new RuntimeException(e);
		}
		return r;
	}
	
	private class SingleDepEdgeScorer implements Callable<Void>{
		final Edge[] r;
		final Instance inst;
		final int beg,end,dep;
		final long[] sink;
		public SingleDepEdgeScorer(Edge[] r, Instance inst, int beg, int end,int dep,long[] sink) {
			this.r = r;
			this.inst = inst;
			this.beg = beg;
			this.end = end;
			this.dep = dep;
			this.sink = sink;
		}
		@Override
		public Void call() throws Exception {
			for(int i=beg;i<end;++i)
				r[i]=create(i);
			return null;
		}
		
		Edge create(int i){
			return fs.createScoredEdge(i, dep, l2i, parameters, symTab, inst, sink);
		}
	}
	
	private class ThreadSafeSingleDepEdgeScorer extends SingleDepEdgeScorer{
		public ThreadSafeSingleDepEdgeScorer(Edge[] r, Instance inst, int beg,int end,int dep, long[] sink) {
			super(r, inst, beg, end, dep, sink);
		}

		Edge create(int i){
			return fs.synchronizedCreateScoredEdge(i, dep, l2i, parameters, symTab, inst, sink);
		}
	}
	
	private class AllEdgeScorer implements Callable<Void>{
		final Edge[][] r;
		final Instance inst;
		final int beg,end;
		final long[] sink;
		
		public AllEdgeScorer(Edge[][] r,Instance inst,int beg,int end){
			this.r=r;
			this.inst=inst;
			this.beg=beg;
			this.end=end;
			this.sink=new long[2000];
		}
		@Override
		public Void call() throws Exception {
			int[] foo=getlr(beg);
			int nextDep=foo[0];
			int nextHead=foo[1];
			int nextTrian=nextDep*(nextDep+1)/2;
			for(int next=beg;next<end;++next){
				r[nextDep][nextHead]=create(nextDep, nextHead);
				if(next==nextTrian){
					++nextDep;
					nextHead=0;
					nextTrian+=nextDep;
				} else {
					++nextHead;
				}
			}
			return null;
		}
		Edge create(int nextDep, int nextHead) {
			return fs.createScoredEdge(nextHead,nextDep,l2i,parameters,symTab,inst,sink);
		}
	}
	
	private class ThreadSafeAllEdgeScorer extends AllEdgeScorer{
		public ThreadSafeAllEdgeScorer(Edge[][] r, Instance inst,int beg,int end) {
			super(r,inst,beg,end);
		}
		Edge create(int nextDep,int nextHead){
			return fs.synchronizedCreateScoredEdge(nextHead, nextDep, l2i, parameters, symTab, inst, sink);
		}
	}
	
	private static int[] getlr(int i){
		int dep=(int) Math.floor(Math.sqrt(2*i));
		final int tnum=dep*(dep+1)/2; //triangular number
		final int fcv;
		if(tnum < i){ //Check if the triangular number is greater or smaller than the number we're working on
			fcv=tnum+1;
			++dep;
		} else {
			fcv=tnum-dep+1;
		}
		int head=i-fcv;
		return new int[]{dep,head};
	}
	
	
	
	
	private static final int SINK_LEN=800;
//	private final long[] SINGLE_EDGE_SINK=new long[SINK_LEN];
	private static final int[] EMPTY_INT_ARR=new int[0];
//	private static final int minIncrement=250;
//	public static long TIME_FIRE=0;
//	public static long TIME_WAIT=0;
//	public static int  TASK_COUNT=0;
	private HOTEdge[] getHOTEdges(FeatureSet fs,SymbolTable symTab,Instance inst,HOTState hotState,Edge[] edges, boolean threadSafe,boolean skip) {
		//XXX it seems impossible to gain something by parallelizing here. Maybe revisit this code when there are *alot* of
		//    higher order features. for now, just do it single threaded....
		long[] sink=new long[SINK_LEN];
		HOTEdge[] r=new HOTEdge[edges.length];
		if(!fs.higherOrder || skip){
			for(int i=0;i<r.length;++i)
				r[i]=new HOTEdge(edges[i], EMPTY_INT_ARR, 0.f);
			return r;
		}
//		if(r.length<=minIncrement){
		if(threadSafe){
			for(int j=0;j<r.length;++j)
				r[j]=fs.synchronizedCreateHOTEdge(l2i, parameters, symTab, inst, sink, hotState, edges[j]);
			return r;
		} else {
			for(int j=0;j<r.length;++j)
				r[j]=fs.createHOTEdge(l2i, parameters, symTab, inst, sink, hotState, edges[j]);
			return r;			
		}
//		}
//		long t0=System.currentTimeMillis();
//		List<Future<Void>> f=new ArrayList<Future<Void>>();
//		List<Callable<Void>> jobs=new ArrayList<Callable<Void>>();
//		int nextBegin=0;
//		int inc=Math.max(minIncrement, r.length/ThreadPoolSingleton.threadCount()+1);
//		while(nextBegin<r.length){
//			int end=nextBegin+inc;
//			if(end>r.length)
//				end=r.length;
//			HOTScorer s=threadSafe?new ThreadSafeHOTScorer(nextBegin, end, inst, hotState, edges, r):new HOTScorer(nextBegin, end, inst, hotState, edges, r);
////			f.add(ThreadPoolSingleton.getInstance().submit(s));
//			jobs.add(s);
//			TASK_COUNT++;
//			nextBegin+=inc;
//		}
//		long t1=System.currentTimeMillis();
////		final int threads;
////		if(edges.length<6)
////			threads=1;
////		else //here we can have the problem that we have like 10 edges and 20 threads, the following should work 
////			threads=Math.min(edges.length/3, ThreadPoolSingleton.threadCount());
////		
////		int nextBegin=0;
////		int increment=r.length/threads;
////		List<Callable<Void>> jobs=new ArrayList<Callable<Void>>();
////		for(int i=0;i<threads-1;++i){
////			HOTScorer s=threadSafe?new ThreadSafeHOTScorer(nextBegin, nextBegin+increment, inst, hotState, edges, r):new HOTScorer(nextBegin, nextBegin+increment, inst, hotState, edges, r);
////			jobs.add(s);
////			nextBegin+=increment;
////		}
////		jobs.add(threadSafe?new ThreadSafeHOTScorer(nextBegin,r.length,inst,hotState,edges,r):new HOTScorer(nextBegin,r.length,inst,hotState,edges,r));
//		
//		try {
//			for(Future<Void> f:ThreadPoolSingleton.getInstance().invokeAll(jobs))
//				f.get();
////			for(Future<Void> fut:f)
////				fut.get();
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//		long t2=System.currentTimeMillis();
//		TIME_FIRE+=t1-t0;
//		TIME_WAIT+=t2-t1;
//		return r;
	}

}
