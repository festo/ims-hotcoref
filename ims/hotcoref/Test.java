package ims.hotcoref;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import ims.hotcoref.data.Chain;
import ims.hotcoref.data.CorefSolution;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Instance;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.decoder.Decoder;
import ims.hotcoref.decoder.HOTEdge;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.io.CorefTreeWriter;
import ims.hotcoref.io.DocumentReader;
import ims.hotcoref.io.DocumentWriter;
import ims.hotcoref.io.ReaderWriterFactory;
import ims.hotcoref.lang.Language;
import ims.hotcoref.markables.GoldStandardChainExtractor;
import ims.hotcoref.mentiongraph.*;
import ims.hotcoref.mentiongraph.gengraph.InstanceCreator;
import ims.hotcoref.util.WordNetInterface;
import ims.util.DBO;
import ims.util.GraphViz;
import ims.util.PrecisionRecall;
import ims.util.ThreadPoolSingleton;
import ims.util.ZipUtil;

public class Test {

	public static ErrorAnalysis test(DocumentReader reader,DocumentWriter writer,Decoder decoder,InstanceCreator ic,File graphOutDir, int beam,boolean ignoreSingletonsGraphOut,boolean drawLatentHeads,boolean ignoreRoot,boolean doErrorAnalysis,CorefTreeWriter predTreeWriter,CorefTreeWriter goldTreeWriter) throws IOException, InterruptedException, ExecutionException{
		if(drawLatentHeads)
			Options.dontInjectGold=true;
		DBO.printWithPrefix("Doc:  ");
		int erase=0;
		int docCount=0;
		boolean outGraph=graphOutDir!=null;
		long t0=System.currentTimeMillis();
		ErrorAnalysis r=(doErrorAnalysis?new ErrorAnalysis():null);
		for(Document d:reader){
			CorefSolution gold=null;
			HOTState latent=null;
			d.initSInst(decoder.symTab);
			if(outGraph || doErrorAnalysis || goldTreeWriter!=null){
				gold=GoldStandardChainExtractor.getGoldCorefSolution(d);
				Instance inst=ic.createTrainingInstance(d);
				latent=drawLatentHeads?getLatentHeads(decoder,d,ic,beam,inst):null;
				if(latent!=null && goldTreeWriter!=null)
					goldTreeWriter.writeTree(latent, d, inst, "Gold");
					
				for(Sentence s:d.sen)
					s.getSpanMap().clear();
			}
			d.clearCorefCols();
			docCount++;
			Instance inst=ic.createTestInstance(d);
			decoder.fs.fillInstance(inst, d, decoder.symTab);
			HOTState prediction=decoder.corefMST(inst, beam);
			if(predTreeWriter!=null)
				predTreeWriter.writeTree(prediction, d, inst,"Pred");
			CorefSolution cs=decoder.hotState2CS(prediction, inst);
			d.setCorefCols(cs.getKey());
			writer.write(d);
			if(outGraph){
				File outFile=new File(graphOutDir,docCount+".svg");
				outGraph(gold,cs,prediction.getHOTEdges(),outFile,inst,ignoreSingletonsGraphOut,latent,beam,ignoreRoot);
			}
			if(doErrorAnalysis){
				r.count(cs, gold, prediction, ic.markableExtractor.extractMarkables(d), inst);
			}
			long time=System.currentTimeMillis()-t0;
			erase=DBO.eraseAndPrint(erase, getInfoString(docCount,time));
			if(Options.DEBUG){
				erase=0;
				DBO.println();
				DBO.printlnNoPrefix("Nodes: "+inst.nodes.length);
			}
		}
		DBO.println();
		return r;
	}
	
	
	static class ErrorAnalysis {
		
		int extrMentions=0;
		int predMentions=0;
		int goldMentions=0;
				
		int meTP=0;
		int meFP=0;
		int meFN=0;
		
		int mpTP=0;
		int mpFP=0;
		int mpFN=0;
		
		
		
		int correctSingletonRootEdges=0;
		int correctDiscourseFirstRootEdges=0;
		int correctNonRootEdges=0;
		int wrongRootEdge=0;
		int wrongEdgeShouldBeSingletonRoot=0;
		int wrongEdgeShouldBeDiscourseFirstRoot=0;
		int wrongEdgeShouldBeOtherHead=0;

		int totalEdges=0;
		
		void count(CorefSolution predCS,CorefSolution goldCS,HOTState pred,Set<Span> extractedMentions,Instance inst){
			Map<Span,Integer> golds2i=goldCS.getSpan2IntMap();
			Map<Span,Integer> preds2i=predCS.getSpan2IntMap();
			
			goldMentions+=golds2i.size(); 
			predMentions+=preds2i.size();
			extrMentions+=extractedMentions.size();
			
			Set<Span> extractGoldUnion=new HashSet<Span>();
			extractGoldUnion.addAll(goldCS.getSpan2IntMap().keySet());
			extractGoldUnion.addAll(extractedMentions);
			int extractGoldIntersect=goldCS.getSpan2IntMap().size()+extractedMentions.size()-extractGoldUnion.size();
			
			meTP+=extractGoldIntersect;
			meFP+=extractedMentions.size()-extractGoldIntersect; 
			meFN+=golds2i.size()-extractGoldIntersect;
			
			Set<Span> predGoldUnion=new HashSet<Span>();
			predGoldUnion.addAll(goldCS.getSpan2IntMap().keySet());
			predGoldUnion.addAll(predCS.getSpan2IntMap().keySet());
			int predGoldIntersect=goldCS.getSpan2IntMap().size()+predCS.getSpan2IntMap().size()-predGoldUnion.size();
			
			mpTP+=predGoldIntersect;
			mpFP+=preds2i.size()-predGoldIntersect;
			mpFN+=golds2i.size()-predGoldIntersect;
			
			
			//Ok, now start looking at edges.
			totalEdges+=pred.getHOTEdges().size();
			Map<Integer,Chain> goldInt2Chain=goldCS.getChainMap();
			for(HOTEdge he:pred.getHOTEdges()){
				boolean isRootEdge=he.getHeadIdx()==0;
				Span depSpan=((MNode) he.getDep(inst)).span;
				if(isRootEdge){
					//Should it go to the root?
					if(golds2i.containsKey(depSpan)){ 
						//Is it the first mention of that chain?
						Chain c=goldInt2Chain.get(golds2i.get(depSpan));
						if(depSpan.getUniqueIntKey()==c.spans.get(0).getUniqueIntKey())
							++correctDiscourseFirstRootEdges;
						else
							++wrongRootEdge;
					} else {
						++correctSingletonRootEdges;
					}
				} else { //Not a root edge
					//Do the head and dependent belong to the same chain?
					Span headSpan=((MNode) he.getHead(inst)).span;
					Integer gDepCID=golds2i.get(depSpan);
					Integer gHeadCID=golds2i.get(headSpan);
					if(gDepCID!=null && gHeadCID!=null && gDepCID.equals(gHeadCID)){
						++correctNonRootEdges;
					} else {
						//Should the head really be root, or some other mention?
						if(gDepCID==null){
							++wrongEdgeShouldBeSingletonRoot;
						} else {
							Chain c=goldInt2Chain.get(golds2i.get(depSpan));
							if(c.spans.get(0).getUniqueIntKey()==depSpan.getUniqueIntKey())
								wrongEdgeShouldBeDiscourseFirstRoot++;
							else
								wrongEdgeShouldBeOtherHead++;
						}
					}
				}
			}
		}
		
		void output(PrintStream out){
			PrecisionRecall mePR=new PrecisionRecall(meTP,meFP,meFN);
			PrecisionRecall mpPR=new PrecisionRecall(mpTP,mpFP,mpFN);
			out.println("Mention Extraction stats: ");
			out.println();
			out.println(mePR.getPRFString());
			out.println();
			out.println();
			out.println("==========================================");
			out.println();
			out.println("Mention Prediction stats: ");
			out.println();
			out.println(mpPR.getPRFString());
			out.println();
			out.println();
			out.println("==========================================");
			out.println();
			out.println("Edge predictions");
			
			out.println(String.format("Correct singleton root:                         %6d   (%5.2f%%)",correctSingletonRootEdges,100.0*correctSingletonRootEdges/totalEdges));
			out.println(String.format("Correct discourse first root:                   %6d   (%5.2f%%)",correctDiscourseFirstRootEdges,100.0*correctDiscourseFirstRootEdges/totalEdges));
			out.println(String.format("Correct inter-mention:                          %6d   (%5.2f%%)",correctNonRootEdges,100.0*correctNonRootEdges/totalEdges));
			out.println(String.format("Wrong root:                                     %6d   (%5.2f%%)",wrongRootEdge,100.0*wrongRootEdge/totalEdges));
			out.println(String.format("Wrong inter-mention (singleton root):           %6d   (%5.2f%%)",wrongEdgeShouldBeSingletonRoot,100.0*wrongEdgeShouldBeSingletonRoot/totalEdges));
			out.println(String.format("Wrong inter-mention (discourse first root):     %6d   (%5.2f%%)",wrongEdgeShouldBeDiscourseFirstRoot,100.0*wrongEdgeShouldBeDiscourseFirstRoot/totalEdges));
			out.println(String.format("Wrong inter-mention (other mention head):       %6d   (%5.2f%%)",wrongEdgeShouldBeOtherHead,100.0*wrongEdgeShouldBeOtherHead/totalEdges));
			out.println();
			out.println(String.format("Total edges:     %d", totalEdges));
		}
		
	}
	
	
	private static String getInfoString(int docCount,long totalTime){
		long tpi=totalTime/docCount;
		return String.format("%-4d  (t: %4d)", docCount,tpi);
	}
	
	private static HOTState getLatentHeads(Decoder decoder, Document d,InstanceCreator ic,int beam,Instance inst) {
		return decoder.getLatentPredictionMST(d, inst, beam);
	}

	private static void outGraph(CorefSolution gold, CorefSolution cs,List<HOTEdge> pmst,File outFile,Instance inst,boolean ignoreSingletons,HOTState latent,int beam,boolean ignoreRoot) {
		Collections.sort(pmst, HOTEdge.HOTEDGE_DEP_COMPARATOR);
		Map<Span,Integer> goldSp2Int=gold.getSpan2IntMap();
		Map<Span,Integer> predSp2Int=cs.getSpan2IntMap();
		GraphViz gv=new GraphViz();
		gv.addln("digraph G {");
		
		for(HOTEdge e:pmst){
			boolean isRootDep=e.getHead(inst).isVirtualNode();
			boolean isSingleton=isRootDep && predSp2Int.get(((MNode)e.getDep(inst)).span)==null;
			if((ignoreSingletons && isSingleton) ||
			   (ignoreRoot && isRootDep))
				continue;
			int fromGoldClusterID=-1;
			int toGoldClusterID=-1;
			if(!e.getHead(inst).isVirtualNode()){
				Integer i=goldSp2Int.get(((MNode) e.getHead(inst)).span);
				if(i!=null)
					fromGoldClusterID=i;
			}
			if(!e.getDep(inst).isVirtualNode()){
				Integer i=goldSp2Int.get(((MNode) e.getDep(inst)).span);
				if(i!=null)
					toGoldClusterID=i;
			}
				
			StringBuilder pred=new StringBuilder("");
			StringBuilder lat=null;
			String predColor="black";
			String headDotNode=node2DotNode(inst.nodes[e.getHeadIdx()],fromGoldClusterID,e.getHeadIdx());
			String depDotNode=node2DotNode(inst.nodes[e.getDepIdx()],toGoldClusterID,e.getDepIdx());
			pred.append(headDotNode);
			pred.append("->");
			pred.append(depDotNode);
			if(latent!=null){
				HOTEdge q=latent.getHOTEdges().get(e.getDepIdx()-1);
				int latentHead=q.getHeadIdx();
				int lHeadClusterId=-1;
				if(inst.nodes[latentHead] instanceof MNode){
					MNode mn=(MNode) inst.nodes[latentHead];
					Integer a=goldSp2Int.get(mn.span);
					lHeadClusterId=a;
				}
				if(latentHead!=e.getHeadIdx()){
					String lHeadDotNode=node2DotNode(inst.nodes[latentHead],lHeadClusterId,latentHead);
					lat=new StringBuilder();
					lat.append(lHeadDotNode);
					lat.append("->");
					lat.append(depDotNode);
					lat.append(" [style=dashed, color=gray, label=\"s=").append(q.score()).append("\"]");
					lat.append(';');
				}

				//Color the predicted edge
				if(latentHead!=e.getHeadIdx()){ //There is a better antecedent
//					if(lHeadClusterId!=fromGoldClusterID ||
//						(lHeadClusterId==fromGoldClusterID && lHeadClusterId))
//						predColor="red";
//					else
//						predColor="pink";
					if(lHeadClusterId!=-1 && lHeadClusterId==fromGoldClusterID)
						predColor="pink";
					else
						predColor="red";
				}
			} else {
				if(e.getHeadIdx()!=0 && toGoldClusterID!=fromGoldClusterID)
					predColor="red";
			}
			pred.append(" [color="+predColor+", label=\"s=").append(e.score()).append("\"]");
			pred.append(';');
			gv.addln(pred.toString());
			if(lat!=null)
				gv.addln(lat.toString());
		}
		gv.addln("}");
		String src=gv.getDotSource();
		gv.writeGraphToFile(gv.getGraph(src, "svg"), outFile);
	}
	
	static String node2DotNode(INode node,int clusterId,int nodeId){
		return "\""+clusterId+"\\n"+node.getDotNodeName(nodeId)+"\"";
	}

	public static void main(String[] args) throws Exception {
		Options options=new Options(args);
		ThreadPoolSingleton.getInstance();
		WordNetInterface.theInstance();
		DocumentReader reader=ReaderWriterFactory.getInputReader(options);
		DocumentWriter writer=ReaderWriterFactory.getOutputWriter(options);
		DBO.println("Reading model");
		Decoder decoder=ZipUtil.loadObjectFromEntry(Decoder.ZIP_ENTRY, options.model, Decoder.class);
		Language lang=ZipUtil.loadObjectFromEntry(Language.ZIP_ENTRY, options.model, Language.class);
		Language.setLanguage(lang);
		InstanceCreator ic=ZipUtil.loadObjectFromEntry(InstanceCreator.ZIP_ENTRY, options.model, InstanceCreator.class);
		CorefTreeWriter goldTreeWriter=null;
		CorefTreeWriter predTreeWriter=null;
		if(options.outIcarusTrees){
			String prefix=options.model.getName();
			File outGoldTree=new File(options.out.getAbsolutePath()+".GOLD.icarus");
			File outPredTree=new File(options.out.getAbsolutePath()+".PRED.icarus");
			goldTreeWriter=new CorefTreeWriter(outGoldTree,prefix);
			predTreeWriter=new CorefTreeWriter(outPredTree,prefix);
		}
		ErrorAnalysis r=test(reader,writer,decoder,ic,options.graphOutDir,options.beam,options.ignoreSingletons,options.drawLatentHeads,options.ignoreRoot,options.errorAnalysis,predTreeWriter,goldTreeWriter);
		writer.close();
		if(options.outIcarusTrees){
			goldTreeWriter.close();
			predTreeWriter.close();
		}
		if(r!=null){
			System.out.println();
			System.out.println("Error analysis");
			r.output(System.out);
			System.out.println();
		}
		
		options.done();
	}
}
