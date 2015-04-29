package ims.hotcoref;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipOutputStream;

import ims.hotcoref.Test.ErrorAnalysis;
import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.Decoder;
import ims.hotcoref.decoder.HOTEdge;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.decoder.Decoder.BeamDUReturn;
import ims.hotcoref.features.FeatureSet;
import ims.hotcoref.io.DocumentReader;
import ims.hotcoref.io.DocumentWriter;
import ims.hotcoref.io.ReaderWriterFactory;
import ims.hotcoref.lang.Language;
import ims.hotcoref.markables.IMarkableExtractor;
import ims.hotcoref.markables.MarkableExtractorFactory;
import ims.hotcoref.mentiongraph.Edge;
import ims.hotcoref.mentiongraph.gengraph.InstanceCreator;
import ims.hotcoref.perceptron.FV3;
import ims.hotcoref.perceptron.IFV;
import ims.hotcoref.perceptron.Long2Int;
import ims.hotcoref.perceptron.Long2IntExact;
import ims.hotcoref.perceptron.Long2IntInterface;
import ims.hotcoref.perceptron.ParametersFloat;
import ims.hotcoref.perceptron.Regularizer;
import ims.hotcoref.perceptron.AbstractGaussianParametersFloat.*;
import ims.hotcoref.perceptron.ParametersFloat.UpdStruct;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.util.ScoringServer;
import ims.hotcoref.util.WordNetInterface;
import ims.util.ArrayFunctions;
import ims.util.ArrayShuffler;
import ims.util.DBO;
import ims.util.Pair;
import ims.util.ThreadPoolSingleton;
import ims.util.Util;
import ims.util.ZipUtil;

public class LearnWeights {

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException{
		Options options=new Options(args);
		DBO.println("Training on: "+options.in);
		DBO.println("Setting up");
		Language.initLanguage(options.lang);
		FeatureSet fs=FeatureSet.getFeatureSet(options); //init this early, so it throws errors early if the feature set is borked
		DocumentReader reader=ReaderWriterFactory.getInputReader(options);
		IMarkableExtractor me=MarkableExtractorFactory.getExtractorS(options);
		if(me.needsTraining())
			me.train(reader,options.count);
		DBO.println("Using markable extractors: "+me.toString());
		InstanceCreator ic=new InstanceCreator(me);
		Long2IntInterface l2i=options.useHashKernel?new Long2Int(options.hashSize):new Long2IntExact(options.hashSize);
		SymbolTable symTab=fs.createSymbolTable();

		WordNetInterface.theInstance();
		ThreadPoolSingleton.getInstance();
		
		int totalDoc=symTab.registerSimple(reader,options.count);
		DBO.println("Registering complex types and creating instances");
		Instance[] instances=symTab.registerComplexAndCreateInstances(reader,ic,totalDoc,fs,!Options.dontClearSpans);
		if(options.treeModel!=null)
			precomputeTrees(instances,options.treeModel,options.beam);
		DBO.println("Feature templates:");
		DBO.printlnNoPrefix(fs.toString(symTab));
		
		System.out.println("Pruned Chains (mentions):    "+ic.totalPrunedChains+" ("+ic.totalPrunedMentions+")");
		System.out.println("Injected mentions:           "+ic.totalInjectedGoldMentions);
		System.out.println("Discarded singleton chains:  "+ic.totalPrunedSingletonChains);
		Regularizer regularizer=Regularizer.getRegularizer(options.regularizerType, options.C);		
		ParametersFloat parameters=options.cw?new CWParametersFloat(l2i.size(),1.0f,.5f,regularizer):(options.arow?new AROWParametersFloat(l2i.size(),1.0f,1.0f,regularizer):new ParametersFloat(l2i.size(),regularizer));
		Decoder decoder=new Decoder(parameters.parameters,l2i,fs,symTab);
		DBO.println("Begin training. Using regularizer: "+regularizer.toString());
		DBO.println("Rootloss: "+options.rootLoss); //+", LossAug: "+options.lossAugmented);
		DBO.println("IC stats:");
		System.out.println("Beam:                        bSize: "+options.beam);
		
		computeMinMaxBushinessAndArcLen(instances);

		if(!options.dontShuffle)
			shuffle(instances,new ArrayShuffler());
		
		if(decoder.symTab.genre!=null)
			updatesByGenre=new int[decoder.symTab.genre.getItems()];
		
		long start=System.currentTimeMillis();

		double updates=0;
		
		for(int iter=0;iter<options.iterations;++iter){
//			DBO.println("Iteration "+iter);
			DBO.printWithPrefix("Iteration "+iter+" \t");
			boolean threadSafe=!l2i.frozen();

			if(options.beam>1 && options.hotDelay<=iter){
				updates=doBeamSearchIteration(instances, options.rootLoss, decoder, parameters, options.beam, options.beamEarlyIter, updates, false,options.delayUpdates,iter,threadSafe);
			} else {
				updates=doFOIteration(instances, l2i, options.rootLoss, options.hotDelay, options.beam,decoder, parameters, options.beamEarlyIter, updates, iter, threadSafe);
			}
			decoder.resetTime();
			DBO.println("Latent tree   bushiness: "+bushiness(instances)+", arclen: "+arcLen(instances));
			if(!fs.higherOrder)
				l2i.freeze();
			if(options.testAtEveryIter && iter+1!=options.iterations){
				l2i.freeze();
				Decoder d2=new Decoder(parameters.averagedCopy((float) updates), l2i, fs, symTab);
				int padding=Integer.toString(options.iterations-1).length();
				File outFile=new File(options.out.toString()+".i"+Util.padZerosFront(padding, iter+1));
				DocumentReader testRead=ReaderWriterFactory.getReader(options.inputFormat, options.in2, options.inGz, options.inputEnc);
				DocumentWriter testWrite=ReaderWriterFactory.getWriter(options.outputFormat, outFile, options.outGz, options.outputEnc);
				Test.test(testRead, testWrite, d2, ic, options.graphOutDir, options.beam, options.ignoreSingletons, options.drawLatentHeads, options.ignoreRoot,false,null,null);
				testWrite.close();
				tryScore(options.in2,outFile,options.scorerHost,options.scorerPort);
				if(fs.higherOrder)
					l2i.unFreeze();
			}
		}
		
		if(updatesByGenre!=null){
			DBO.println();
			DBO.println("Genre mapping: "+decoder.symTab.genre.mapping2String());
			DBO.println("Updates by genre");
			//count the number of docs in each genre
			int[] docCounts=new int[updatesByGenre.length];
			for(Instance inst:instances)
				++docCounts[inst.genre];

			for(int i=0;i<docCounts.length;++i){
				if(docCounts[i]==0)
					continue;
				String s=String.format("%-2d  %-10d   %7.4f", i,updatesByGenre[i],((double)updatesByGenre[i])/(docCounts[i]*options.iterations));
				System.out.println(s);
			}
		}
		DBO.println();
		DBO.println("Time doing iterations: "+Util.insertCommas(System.currentTimeMillis()-start));
		DBO.println("Non-zero weights docs (0 th): "+parameters.countNZ());
		DBO.println("Non-zero weights docs ("+ParametersFloat.F_TH+"f th): "+parameters.countNZ2());
		DBO.println("Size parameters: "+parameters.parameters.length);
		
		l2i.freeze();
		parameters.average((float) updates);
		
		saveModel(decoder,ic,options.model);
		if(options.in2!=null){
			DBO.println();
			DBO.println("Testing model: "+options.in2.toString()+" -> "+options.out.toString());
			DocumentReader testRead=ReaderWriterFactory.getReader(options.inputFormat, options.in2, options.inGz, options.inputEnc);
			DocumentWriter testWriter=ReaderWriterFactory.getOutputWriter(options);
			ErrorAnalysis ea=Test.test(testRead, testWriter, decoder, ic,options.graphOutDir,options.beam,options.ignoreSingletons,options.drawLatentHeads,options.ignoreRoot,options.errorAnalysis,null,null);
			if(ea!=null){
				System.out.println();
				ea.output(System.out);
				System.out.println();
			}
			System.out.println("Scoring cmd:");
			System.out.println("~/CONLL12-SCORER/scorer.pl all "+options.in2.getAbsolutePath()+" "+options.out.getAbsolutePath()+" none &> "+options.out.getAbsolutePath()+".scores &");
			testWriter.close();
			tryScore(options.in2,options.out,options.scorerHost,options.scorerPort);
			if(options.testAtEveryIter){
				System.out.println("Scoring rest:");
				System.out.println("for i in `seq -w 1 "+(options.iterations-1)+"`; do echo -n .; ~/CONLL12-SCORER/scorer.pl all "+options.in2.getAbsolutePath()+" "+options.out.getAbsolutePath()+".i${i} none &> "+options.out.getAbsoluteFile()+".i${i}.scores; done; echo");
			}
		}
		options.done();
	}

	private static void tryScore(File gold, File pred, String scorerHost,int scorerPort) {
		if(scorerHost==null)
			return;
		try {
			File output=new File(pred.getAbsolutePath()+".scores");
			ScoringServer.sendJob(gold, pred, output, scorerHost, scorerPort);
		} catch(Exception e){
			e.printStackTrace();
			System.err.println();
			System.err.println("Failed to issue scoring job to server: "+scorerHost+" : "+scorerPort);
		}
	}

	private static void computeMinMaxBushinessAndArcLen(Instance[] instances) {
		int den=0;
		int maxNum=0;
		int minNum=0;
		
		int alDen=0;
		int alMaxNum=0;
		int alMinNum=0;
		
		for(Instance inst:instances){
			for(int[] cn:inst.chainNodes){
				minNum+=1+(cn.length-1)*2;
				for(int p=0;p<cn.length;++p)
					maxNum+=p+1;
				for(int p=1;p<cn.length;++p){
					++alDen;
					alMinNum+=cn[p]-cn[p-1];
					alMaxNum+=cn[p]-cn[0];
				}
			}
			den+=inst.nodes.length-1;
		}
		double max=((double) maxNum)/den;
		double min=((double) minNum)/den;
		
		double alMax=((double) alMaxNum)/alDen;
		double alMin=((double) alMinNum)/alDen;
		
		DBO.println();
		DBO.println("Bushiness: (min: "+min+  ") (max: "+max+  ")");
		DBO.println("Arclen:    (min: "+alMin+") (max: "+alMax+")");
		DBO.println();
	}

	private static void precomputeTrees(Instance[] instances, File treeModel,int beam) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
		DBO.println("Loading treemodel from "+treeModel.toString());
		Decoder decoder=ZipUtil.loadObjectFromEntry(Decoder.ZIP_ENTRY, treeModel, Decoder.class);
		for(Instance inst:instances){
			HOTState hs=decoder.corefMST(inst, beam);
			inst.preHeads=hs.getHeads();
		}
	}
	
	private static int[] updatesByGenre;

	private static double doBeamSearchIteration(Instance[] instances,float rootLoss,Decoder decoder,ParametersFloat parameters,int beam, boolean iterative, double upd, boolean skipHOT,boolean delayUpdates,int iter,boolean threadSafe)throws InterruptedException, ExecutionException {
		DBO.printlnNoPrefix("Beam search (LaSO: "+iterative+",  delay updates: "+delayUpdates+")");
		IFV<FV3> goldFV=new FV3();
		IFV<FV3> predFV=new FV3();
		int erase=0;
		int docCount=0;
		double accBeamStop=0;
		int beamAllWay=0;
		
		float totUpdates=0;
		int noupd=0;

		int totalFail=0;
		
		float totLoss=0;
		long timeUpdates=0;
		long timeSearchAndScore=0;
		long totalTime=0;
		
		int sameLatent=0;			
		int totalSTEdges=0;
		int sameAsBeforeLatentEdges=0;
		
		int accEarlySkipped=0;
		int accTotalNodes=0;
		
		for(Instance inst:instances){
			docCount++;
			int depStart=1;
			float loss;
			boolean cont=iterative;
			long tInst0=System.currentTimeMillis();
			boolean anyUpdate=false;
			int stopped=0;
			int updatesThisInstance=0;
			HOTState finalGold;
			if(delayUpdates){
				BeamDUReturn bdur=decoder.advanceIterativeEarlyBeamStateDU(inst,beam,false,threadSafe,rootLoss,upd);
				finalGold=bdur.gold;
				if(bdur.ups>0){
					long t0=System.currentTimeMillis();
					updatesThisInstance=bdur.ups;
					if(updatesByGenre!=null)
						updatesByGenre[inst.genre]+=updatesThisInstance;
					anyUpdate=true;
					totLoss+=bdur.loss;
					parameters.update(bdur.predFV, bdur.goldFV, bdur.loss, upd);
					long t1=System.currentTimeMillis();
					timeUpdates+=t1-t0;
				}
				stopped=inst.nodes.length-1;
			} else {
				do {
					long t0=System.currentTimeMillis();
					Pair<HOTState,HOTState> p=decoder.getIterativeEarlyBeamState2(inst, beam, depStart,skipHOT);
					long t1=System.currentTimeMillis();
					HOTState gold=p.getLeft();
					HOTState pred=p.getRight();
					if(gold==null){
						loss=0.f;
						cont=false;
						finalGold=pred;
					}else{
						List<HOTEdge> goldEdges=new ArrayList<HOTEdge>(gold.getHOTEdges()); //We only need this if we might modify these lists when
						List<HOTEdge> predEdges=new ArrayList<HOTEdge>(pred.getHOTEdges()); //we compute the loss
						//					loss=getLoss4(goldEdges,predEdges,rootLoss,inst);
						loss=getLoss5(goldEdges,predEdges,rootLoss,inst);
						finalGold=gold;
						totLoss+=loss;
						int edges=pred.getHOTEdges().size(); //When we are done this is == inst.nodes.length-1 (a spanning tree has nodes-1 edges)
						if(edges+1<inst.nodes.length)
							depStart=edges+1;
						else
							cont=false;

						fillVector(goldFV,goldEdges);
						fillVector(predFV,predEdges);
						UpdStruct us=new UpdStruct(pred.score(),gold.score(),loss,upd+gold.getHOTEdges().size(),goldFV.getDistVector(predFV));
//						UpdStruct us=new UpdStruct(pred.score(),gold.score(),loss,upd,goldFV.getDistVector(predFV));
						++updatesThisInstance;
						if(!parameters.update(us))
							++totalFail;
						anyUpdate=true;
						if(updatesByGenre!=null)
							++updatesByGenre[inst.genre];
					}
					stopped=pred.getHOTEdges().size();
					long t2=System.currentTimeMillis();
					timeSearchAndScore+=t1-t0;
					timeUpdates+=t2-t1;
				} while(cont);
			}

			if(finalGold.getHOTEdges().size()==inst.nodes.length-1){ //Track how gold changes (non-repeating early updates are not handled properly) 
				int[] heads=finalGold.heads;
				int sameLatentEdges=ArrayFunctions.countSame(heads, inst.lastIterHeads,1,inst.nodes.length);
				if(sameLatentEdges==heads.length-1)
					sameLatent++;
				totalSTEdges+=heads.length-1;
				sameAsBeforeLatentEdges+=sameLatentEdges;
				inst.lastIterHeads=heads;
			}
			
//			GraphUtil.graphToDot(finalGold.getHOTEdges(), null, new File("/home/users0/anders/storage/scratch/anders/qqq4/i"+iter+"-i"+docCount+".gold.svg"), "svg", true, true, inst);
			
			if(!anyUpdate)
				noupd++;
			else
				totUpdates+=updatesThisInstance;
			
			accTotalNodes+=inst.nodes.length-1;
			accEarlySkipped+=inst.nodes.length-1-stopped;
			
			if(stopped==inst.nodes.length-1){
				++beamAllWay;
				accBeamStop+=100.0d;
			} else {
				accBeamStop+=100.0d*stopped/inst.nodes.length-1;
			}
			upd+=inst.nodes.length-1;
//			++upd;
			totalTime+=System.currentTimeMillis()-tInst0;
			if((docCount-1)%111==0 || Options.DEBUG){
				erase=DBO.eraseAndPrint(erase, infoStringBeamIter(docCount, totUpdates, noupd, totLoss, timeUpdates, timeSearchAndScore, totalTime,decoder,beamAllWay,accBeamStop,sameLatent,sameAsBeforeLatentEdges,totalSTEdges));
				if(Options.DEBUG){
					erase=0;
					DBO.printlnNoPrefix(" nz2: "+parameters.countNZ2()+", nz1: "+parameters.countNZ());
				}
			}
		}
		DBO.eraseAndPrint(erase, "EOI: "+infoStringBeamIter(docCount, totUpdates, noupd, totLoss, timeUpdates, timeSearchAndScore, totalTime, decoder,beamAllWay,accBeamStop,sameLatent,sameAsBeforeLatentEdges,totalSTEdges)+" | NZ2: "+parameters.countNZ2()+"eu-misses: "+String.format("%d/%d = %.2f", accEarlySkipped,accTotalNodes,100.0*accEarlySkipped/accTotalNodes));
		if(totalFail>0)
			DBO.println("Update failures: "+totalFail);
		DBO.println();
		return upd;
	}
	
	private static String infoStringBeamIter(int docCount, float totUpdates, int noupd, float totLoss, long timeUpdates, long timeSearchAndScore, long totalTime, Decoder decoder, int beamAllWay, double accBeamStop,int sameLatentTrees,int sameLatentEdges,int totalEdges) {
		//docCount
		float l=(totLoss/docCount);
		//noUpd
		float updPi=totUpdates/docCount;
		double bs=accBeamStop/docCount;
		int ba=beamAllWay;
		long t=totalTime/docCount;
		long lAu=timeUpdates/docCount;
		long G=decoder.beiBeamGoldState/docCount;
		long P=decoder.beiPredictState/docCount;
		int tU=(int)totUpdates;
		int sameT=sameLatentTrees;
		float sameE=100.f*sameLatentEdges/totalEdges;
		return String.format("%-5d  l:%5.1f, noup:%4d, upd/i:%5.1f, bs:%5.1f, ba:%4d  |  t %5d (G %5d,  P %5d, l&u %3d) | tu %6d | l-same %4d, e-same %4.2f%%", 
		                  docCount,       l,    noupd,       updPi,       bs,     ba,        t,     G,      P,     lAu,       tU,       sameT,    sameE);
	}

//
//	private static float getLoss4(List<HOTEdge> goldEdges,List<HOTEdge> predEdges, float rootLoss,Instance inst) {
//		if(goldEdges.size()!=predEdges.size())
//			throw new Error("different number of edges");
//		Iterator<HOTEdge> gIt=goldEdges.iterator();
//		Iterator<HOTEdge> pIt=predEdges.iterator();
//		float loss=0.f;
//		while(gIt.hasNext()){
////			HOTEdge g=
//			gIt.next();
//			HOTEdge p=pIt.next();
//			if(!Decoder.sameChain(p.getDepIdx(), p.getHeadIdx(), inst)){
//				loss+=p.getHeadIdx()==0?rootLoss:1.0f;
//			} else {
//				gIt.remove();
//				pIt.remove();
//			}
//		}
//		return loss;
//	}
	public static float getLoss5(List<HOTEdge> goldEdges,List<HOTEdge> predEdges,float rootLoss,Instance inst){
		if(goldEdges.size()!=predEdges.size())
			throw new Error("different number of edges");
		Iterator<HOTEdge> gIt=goldEdges.iterator();
		Iterator<HOTEdge> pIt=predEdges.iterator();
		float loss=0.f;
		while(gIt.hasNext()){
			HOTEdge g=gIt.next();
			HOTEdge p=pIt.next();
			if(g.getHeadIdx()!=p.getHeadIdx())
				loss+=p.getHeadIdx()==0?rootLoss:1.f;
		}
		return loss;
	}


//	static final boolean PRECOMPUTE_GUIDED_LATENT_HEADS=true;
//	private static void trainGuided(DocumentReader reader, Instance[] instances, Long2IntInterface l2i, FeatureSet fs, int iters, Regularizer regularizer,float rootLoss,Decoder decoder,ParametersFloat parameters){
//		DBO.println("Doing guided learning");
//		DBO.println("Genre mapping: "+decoder.symTab.genre.mapping2String());
//		double upd=iters*instances.length+1;
//		//make learning rate dependent on number of mentions that need a head too
//		int mentionsThatNeedAHead=0;
//		for(Instance inst:instances)
//			mentionsThatNeedAHead+=inst.nodes.length-1; //Root doesn't need head (in theory first mention is never wrong either, but easier to leave this)
//		upd=iters*mentionsThatNeedAHead+1;
//		for(int iter=0;iter<iters;++iter){
//			DBO.println("Iteration "+iter);
//			int erase=0;
//			int docCount=0;
//
//			int sameLatent=0;			
//			int sameAsBeforeLatentEdges=0;
//			
//			int totUpdates=0;
//			int correctTrees=0;
//			int totalFail=0;
//
//			int totalEdges=0;
//			float totLoss=0;
//			
//			long tstart=System.currentTimeMillis();
//			for(Instance inst:instances){
////				if(upd<1)
////					throw new Error("NEGATIVE LEARNING RATE!!!");
//				docCount++;
//				GuidedReturn o=decoder.guidedLearn(inst,regularizer,rootLoss,parameters,upd,PRECOMPUTE_GUIDED_LATENT_HEADS,inst.preHeads);
//				upd-=inst.nodes.length-2;
//				totalEdges+=inst.nodes.length-1;
//				totUpdates+=o.updates;
//				totLoss+=o.accLoss;
//				if(o.fail>0){
//					totalFail+=o.fail;
//					System.out.println("Fail at doc "+(docCount-1)+", genre: "+inst.genre);
//				}
//				if(o.updates==0)
//					++correctTrees;
//				
//				{   //Check how latent tree differs from last
//					int[] heads=o.state.getHeads();
//					int sameLatentEdges=ArrayFunctions.countSame(heads, inst.lastIterHeads);
//					if(sameLatentEdges==heads.length)
//						sameLatent++;
//					sameAsBeforeLatentEdges+=sameLatentEdges;
//					inst.lastIterHeads=heads;
//				}
//				
//				//print out some stats... (sometimes)
////				if((docCount-1)%111==0){
//					erase=DBO.eraseAndPrint(erase, String.format("%-5d  %s", docCount,avgStringGuided(docCount,totalEdges,correctTrees,totUpdates,totLoss,System.currentTimeMillis()-tstart)));
////				}
//				--upd;
//			}
//			long totalTime=System.currentTimeMillis()-tstart;
//			DBO.eraseAndPrint(erase, String.format("%-5d  ||  EOI  %s", docCount,avgStringGuided(docCount,totalEdges,correctTrees,totUpdates,totLoss,totalTime)));
//			DBO.println();
//			System.out.println(String.format("t: ed %7s lat %7s | nz2 %8d | fail: %4d (count: %4d | dist: %3d) |lsame  t: %4d, e: %5.1f | time: %s",Util.insertCommas(decoder.scoreEdgeTime),Util.insertCommas(decoder.latentTime),parameters.countNZ(),totalFail,decoder.countBreaks,decoder.failBreaks,sameLatent,100.0*sameAsBeforeLatentEdges/totalEdges,Util.insertCommas(totalTime)));
//			decoder.resetTime();
//		}
//	}
//	
//	static String avgStringGuided(int docs,int edges,int correctTrees,int updates,float loss,long time){
//		return String.format("co: %4d,upd: %6d | lod %5.1f,loe%5.2f | time %d", correctTrees,updates,loss/docs,loss/edges,time/docs);
//	}
	
//	private static double train2(DocumentReader reader, Instance[] instances, Long2IntInterface l2i, FeatureSet fs, int iters, Regularizer regularizer,File graphOutDir,float rootLoss,boolean augmentLoss,int hotDelay,int beam,Update update,int mvType,Decoder decoder,ParametersFloat parameters,boolean iterEarlyBeam,boolean doShuffle) throws IOException, InterruptedException, ExecutionException, Error {
//		double upd=0.0;
//		if(graphOutDir!=null && !graphOutDir.isDirectory())
//			graphOutDir=null;
//		for(int iter=0;iter<iters;++iter){
//			DBO.println("Iteration "+iter);
//			boolean threadSafe=!l2i.frozen();
//			upd = doFOIteration(instances, l2i, rootLoss, hotDelay, beam, update, mvType, decoder, parameters,	iterEarlyBeam, upd, iter, threadSafe);
//			if(l2i instanceof Long2IntExact)
//				DBO.printlnNoPrefix("l2i next: "+((Long2IntExact) l2i).getNext());
//			decoder.resetTime();
//			if(!fs.higherOrder)
//				l2i.freeze();
//		}
////		if(update==Update.MaxVio)
////			dumpMaxVioHisto(decoder);
//		DBO.println();
//		DBO.println("Done");
//		return upd;
//	}

	private static double doFOIteration(Instance[] instances,Long2IntInterface l2i,float rootLoss,int hotDelay,int beam,Decoder decoder, ParametersFloat parameters, boolean iterEarlyBeam,double upd, int iter, boolean threadSafe) throws Error, InterruptedException, ExecutionException {
		DBO.printlnNoPrefix("First order iteration");
		FV3 act=new FV3();
		FV3 pred=new FV3();
//		IFV<FV2> act=new FV2();
//		IFV<FV2> pred=new FV2();
//		IFV<FV> act=new FV();
//		IFV<FV> pred=new FV();
		
		double accLoss=0;
		double accCorr=0;
		long accTime=0;
		int noUpd=0;
		
		int erase=0;
		int docCount=0;
		
		long scoreEdgesTime=0;
		long lossAndUpdateTime=0;
		long decTime=0;
		long latentTime=0;
		
		int sameLatent=0;			
		int totalSTEdges=0;
		int sameAsBeforeLatentEdges=0;
		
		int beamAllWay=0;
		double accBeamStop=0;
		
		long tstart=System.currentTimeMillis();
		for(Instance inst:instances){
			long t0=System.currentTimeMillis();
			docCount++;
			boolean skipHOT=iter<hotDelay;
			//Score edges
			Edge[][] allEdgesScored=decoder.scoreAllFOEdges(inst,threadSafe);
			//Make prediction and latent tree prediction
			long t1=System.currentTimeMillis();
			scoreEdgesTime+=t1-t0;
			//Get latent tree
			HOTState goldState=decoder.getLatentMST(allEdgesScored, inst, threadSafe, skipHOT,inst.preHeads,1);
			{   //Check how latent tree differs from last
				int[] heads=goldState.getHeads();
				int sameLatentEdges=ArrayFunctions.countSame(heads, inst.lastIterHeads,1,inst.nodes.length);
				if(sameLatentEdges==heads.length-1)
					sameLatent++;
				totalSTEdges+=heads.length-1;
				sameAsBeforeLatentEdges+=sameLatentEdges;
				inst.lastIterHeads=heads;
			}
//			HOTState seedState=new HOTState(inst.nodes.length);
			long t2=System.currentTimeMillis();
			latentTime+=t2-t1;
//			do {
				long tt2=System.currentTimeMillis();
				final HOTState predState;
//				if(update==Update.Standard){
					predState=decoder.predictMSTNoBeam(allEdgesScored, inst, threadSafe,skipHOT);
//				} else if(update==Update.Early){
//					predState=decoder.getEarlyBeamState(allEdgesScored,inst,threadSafe,skipHOT,beam,goldState,seedState);
//				} else if(update==Update.MaxVio){
//					predState=decoder.getMaxVioBeamState(allEdgesScored,inst,threadSafe,skipHOT,beam,goldState,rootLoss,mvType,seedState);
//				} else {
//					throw new Error("!");
//				}
				long tt3=System.currentTimeMillis();
				decTime+=tt3-tt2;
				if(predState.getHOTEdges().size()==goldState.getHOTEdges().size()){
					++beamAllWay;
					accBeamStop+=100.0;
				} else {
					accBeamStop+=100.0*predState.getHOTEdges().size()/goldState.getHOTEdges().size();
				}

				//				{ //Sanity checks for connectedness of pred & gold, this can go...
				//					sanityCheckConnected(goldState.getHOTEdges());
				//					sanityCheckConnected(predState.getHOTEdges());
				//				}
				//				long t4=System.currentTimeMillis();
				List<HOTEdge> predEdges=predState.getHOTEdges();
				List<HOTEdge> goldEdges=goldState.getHOTEdges().subList(0, predEdges.size());
//				int predProgress=predEdges.size();
				Pair<Float,Double> p=getLoss2(goldEdges,predEdges,rootLoss,inst);
				float structLoss=p.getLeft();
				double corr=p.getRight();

				accLoss+=structLoss;
				accCorr+=corr;
				if(Options.DEBUG){
					erase=0;
					DBO.println();
				}
				if(structLoss==0.f){
					noUpd++;
					lossAndUpdateTime+=System.currentTimeMillis()-tt3;
				} else {
//						noUpdates++;
					fillVector(act,goldEdges);
					fillVector(pred,predEdges);
					//					DBO.println("GFSIZE: "+gFSize+" | PFSIZE: "+pFSize);
					UpdStruct us=new UpdStruct(predState.score(),goldState.score(),structLoss,upd,act.getDistVector(pred));
//					TIntIntHashMap dv=;
//					parameters.update(act, pred,upd, structLoss);
					parameters.update(us);
					if(updatesByGenre!=null)
						++updatesByGenre[inst.genre];
					lossAndUpdateTime+=System.currentTimeMillis()-tt3;
				}
				upd+=inst.nodes.length-1;
//				if(iterEarlyBeam && predState.getHOTEdges().size()<goldState.getHOTEdges().size()){
//					allEdgesScored=decoder.scoreEdges(inst,threadSafe);
//					goldState=decoder.getLatentMST(allEdgesScored, inst, threadSafe, skipHOT,inst.preHeads,1);
//					seedState=goldState.copyUntil(predProgress);
//				}
//				else {
//					break;
//				}
//			} while(true);
			accTime+=System.currentTimeMillis()-t0;
			if((docCount-1)%111==0 || Options.DEBUG){
				erase=DBO.eraseAndPrint(erase, Integer.toString(docCount));
				erase+=DBO.printNoPrefix(getAvgString(noUpd, accLoss, accCorr, accTime, docCount,accBeamStop,beamAllWay,0));
			}
		}
		double latentESame=100.0*sameAsBeforeLatentEdges/totalSTEdges;
		DBO.eraseAndPrint(erase, Integer.toString(docCount)+" EOI "+getAvgString(noUpd, accLoss, accCorr, accTime, docCount,accBeamStop,beamAllWay,0)+"| nz2 "+parameters.countNZ2()+" | l-same: "+sameLatent+", e-same "+String.format("%.2f",latentESame)+"%| time: "+Util.insertCommas(System.currentTimeMillis()-tstart));
		DBO.println();
		DBO.printlnNoPrefix(String.format("t: ed:%9s | p:%9s (hot:%9s | p:%8s) | lat: %8s | l&u:%8s", 
				     Util.insertCommas(scoreEdgesTime),Util.insertCommas(decTime),Util.insertCommas(decoder.hoTime),Util.insertCommas(decoder.predTime),Util.insertCommas(latentTime),Util.insertCommas(lossAndUpdateTime)));
		return upd;
	}

//	private static void dumpMaxVioHisto(Decoder decoder) {
//		System.out.println();
//		System.out.println("Max vio vs early histo");
//		int[] keys=decoder.maxVioHisto.keys();
//		Arrays.sort(keys);
//		for(int k:keys)
//			System.out.printf("%-4d   %d\n",k,decoder.maxVioHisto.get(k));
//		System.out.println();
//	}

//	private static void sanityCheckConnected(List<HOTEdge> edges) {
//		DisjointSetZeroToNRankPathComp dset=new DisjointSetZeroToNRankPathComp(edges.size()+1);
//		for(HOTEdge e:edges){
//			int a=dset.find(e.getHeadIdx());
//			int b=dset.find(e.getDepIdx());
//			if(a==b)
//				throw new Error("cycle");
//			dset.union(a, b);
//		}
//		if(dset.getSubsets()!=1)
//			throw new Error("not connected");
//	}

	private static void saveModel(Decoder decoder, InstanceCreator ic, File modelFile) throws IOException {
		if(modelFile==null){
			DBO.println("Skipping saving model");
			return;
		}
		DBO.println("Saving model to "+modelFile.toString());
		ZipOutputStream zos=ZipUtil.openZOS(modelFile);
		ZipUtil.writeStringTXT(zos, FeatureSet._FEATURES_ZIP_ENTRY, decoder.fs.getFeatureNamesForTXT(decoder.symTab));
		ZipUtil.writeObjectAsZipEntry(zos, Decoder.ZIP_ENTRY, decoder);
		ZipUtil.writeObjectAsZipEntry(zos, InstanceCreator.ZIP_ENTRY, ic);
		ZipUtil.writeObjectAsZipEntry(zos, Language.ZIP_ENTRY, Language.getLanguage());
		ZipUtil.writeStringTXT(zos, Language.ZIP_TXT_ENTRY, Language.getLanguage().getLang());
		zos.close();
	}

	private static String getAvgString(int noupd,double accLoss, double accCorr,long accTime, int docCount,double accBeamStop,int beamAllWay,int maxVioEUDiff) {
		return String.format("noup:%4d, f:%4d | lo:%6.1f,co:%4.1f,bs:%4.1f,mvd:%5.1f| time:%4d", noupd, beamAllWay, ((double) accLoss/docCount),((double) accCorr/docCount),((double) accBeamStop/docCount),(float)maxVioEUDiff/docCount,(int) Math.ceil((double) accTime/docCount));
	}



	public static void fillVector(IFV<?> fv, Collection<HOTEdge> edges) {
		fv.clear();
		for(HOTEdge e:edges){
			fv.add(e.getFO());
			fv.add(e.getHO());
		}
	}
		
	private static Pair<Float,Double> getLoss2(List<HOTEdge> goldEdges,List<HOTEdge> predEdges,float rootLoss,Instance inst){
		if(predEdges.size()!=goldEdges.size())
			throw new Error("different number of edges");
		Collections.sort(goldEdges,HOTEdge.HOTEDGE_DEP_COMPARATOR);
		Collections.sort(predEdges,HOTEdge.HOTEDGE_DEP_COMPARATOR);
		Iterator<HOTEdge> gIt=goldEdges.iterator();
		Iterator<HOTEdge> pIt=predEdges.iterator();
		int tot=goldEdges.size();
		float loss=0f;
		int errEdges=0;
		while(gIt.hasNext()){
			HOTEdge g=gIt.next();
			HOTEdge p=pIt.next();
			int dep=g.getDepIdx();
			if(dep!=p.getDepIdx())
				throw new Error("!");
			int gHead=g.getHeadIdx();
			int pHead=p.getHeadIdx();
//			if(gHead!=pHead && inst.nodeToChainNodeArr[gHead]!=inst.nodeToChainNodeArr[pHead]){//XXX this is needed for the fixed heads for the first order model, but harmful for beam search.
			if(gHead!=pHead){ 
				errEdges++;
				boolean isRootEdge=p.getHead(inst).isVirtualNode();
				loss+=isRootEdge?rootLoss:1.0f;
			}
//			} else {
//				gIt.remove();
//				pIt.remove();
//			}
		}
		if(gIt.hasNext() || pIt.hasNext())
			throw new Error("!");
		Double co=100.0-100.0*errEdges/tot;
		return new Pair<Float,Double>(loss,co);
	}
	
	private static void shuffle(Instance[] insts,ArrayShuffler arrayShuffler){
		DBO.println("Shuffling instances");
		arrayShuffler.shuffle(insts);
	}
	
	public static float arcLen(Instance[] insts){
		int ed=0;
		int dist=0;
		for(Instance inst:insts){
			for(int i=1;i<inst.nodes.length;++i){
				if(inst.lastIterHeads[i]>0){
					ed++;
					dist+=i-inst.lastIterHeads[i];
				}
			}
		}
		return (float)dist/ed;
	}

	public static float bushiness(Instance[] insts){
		int edges=0;
		int depth=0;
		for(Instance inst:insts){
			for(int i=1;i<inst.nodes.length;++i){
				int d=distToRoot(i,inst);
				if(d!=-1){
					depth+=d;
					++edges;
				}
			}
		}
		return (float)depth/edges;
	}

	private static int distToRoot(int i, Instance inst) {
		if(inst.lastIterHeads[i]==-1)
			return -1;
		int d=0;
		while(i!=0){
			++d;
			i=inst.lastIterHeads[i];
		}
		return d;
	}
}
