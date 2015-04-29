package ims.hotcoref.markables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ims.hotcoref.Options;
import ims.hotcoref.data.CorefSolution;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.io.DocumentReader;
import ims.hotcoref.io.ReaderWriterFactory;
import ims.hotcoref.lang.Arabic;
import ims.hotcoref.lang.Chinese;
import ims.hotcoref.lang.English;
import ims.hotcoref.lang.Language;
import ims.util.ThreadPoolSingleton;

//"it"
//
//Token-count: 10307
//TP=2650 FP=864 FN=1630
//Prec:	 75.413
//Rec: 	 61.916
//F1:  	 68.001
//
//DropCount: 3514
//
//"you"
//
//Token-count: 11297
//TP=1955 FP=685 FN=1751
//Prec:	 74.053
//Rec: 	 52.752
//F1:  	 61.614
//
//DropCount: 2640
//
//"it"+"you"
//
//Token-count: 21604
//TP=4605 FP=1549 FN=3381
//Prec:	 74.829
//Rec: 	 57.663
//F1:  	 65.134
//
//DropCount: 6154
//
//"it"+"you", th=0.6
//
//Token-count: 21604
//TP=4374 FP=1348 FN=3612
//Prec:	 76.442
//Rec: 	 54.771
//F1:  	 63.817
//
//DropCount: 5722
//
//"it"+"you", th=0.7
//
//TP=4127 FP=1133 FN=3859
//Prec:	 78.460
//Rec: 	 51.678
//F1:  	 62.313
//
//DropCount: 5260
//
//"it"+"you", th=0.8
//Token-count: 21604
//TP=3820 FP=936 FN=4166
//Prec:	 80.320
//Rec: 	 47.834
//F1:  	 59.959
//
//DropCount: 4756
//
//"it"+"you", th=0.9
//Token-count: 21604
//TP=3327 FP=666 FN=4659
//Prec:	 83.321
//Rec: 	 41.660
//F1:  	 55.547
//
//DropCount: 3993

public class NonReferentialPruner extends AbstractMarkableExtractor{
	private static final long serialVersionUID = 1L;

	private static final int FOLDS=10;
	
	private final NonReferentialClassifier[] cv;
	private final NonReferentialClassifier test;
	
	private static final Map<String,Integer> docNameToIndex=new HashMap<String,Integer>();
	
	public NonReferentialPruner(){
		cv=new NonReferentialClassifier[FOLDS];
		Set<String> tokens=getTokenSet();
		Map<String,Double> customTokenTh=getCustomTokenAnaphoricityTh(tokens);
		String l=Language.getLanguage().getLang();
		for(int i=0;i<FOLDS;++i)
			cv[i]=new NonReferentialClassifier(l, Options.anaphoricityThreshold,tokens,customTokenTh);
		test=new NonReferentialClassifier(l,Options.anaphoricityThreshold,tokens,customTokenTh);
		System.out.println("Created NonReferentialPruner:: "+this.toString());
	}
	
	@Override
	public boolean needsTraining() {
		return true;
	}

	@Override
	public void train(DocumentReader reader,int count) {
		int i=-1;
		String lastDoc="";
		int docCount=0;
		for(Document d:reader){
			docCount++;
			if(!lastDoc.equals(d.docName)){
				i++;
				i%=FOLDS;
				docNameToIndex.put(d.docName, i);
				lastDoc=d.docName;
			}
			//Then feed this document as a training instance to every classifier except cv[i]
			for(Sentence s:d.sen){
				test.extractTrainingInstances(s.wholeForm, s.tags, s.corefCol,d.genre);
				for(int q=0;q<FOLDS;++q){
					if(q==i)
						continue;
					cv[q].extractTrainingInstances(s.wholeForm, s.tags, s.corefCol,d.genre);
				}
			}
			if(docCount>count)
				break;
		}
		ExecutorService threadPool=ThreadPoolSingleton.getInstance().getThreadPool();
		List<Callable<Void>> trainJobs=new ArrayList<Callable<Void>>();
		trainJobs.add(new TrainJob(test));
		for(int q=0;q<FOLDS;++q)
			trainJobs.add(new TrainJob(cv[q]));
		try {
			for(Future<Void> f:threadPool.invokeAll(trainJobs)){
				f.get();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("!");
		}
	}
	
	static class TrainJob implements Callable<Void> {
		private final ITokenNonReferentialClassifier a;
		TrainJob(ITokenNonReferentialClassifier q){
			this.a=q;
		}
		@Override
		public Void call() throws Exception {
			a.train();
			System.out.println(new Date()+": Done training anaphoricity classifier.");
			return null;
		}
	}

	@Override
	public void extractMarkables(Sentence s, Set<Span> sink,String docName) {
		NonReferentialClassifier q = getClassifier(docName);
		double[] r=q.classifyGetProbs(s.wholeForm, s.tags, docName.substring(0,2));
		for(int i=1;i<r.length;++i){
			if(r[i]>0){
				Span sp=s.getSpan(i, i);
				sp.anaphoricityPr=r[i];
				double th=(q.customTokenTh==null?q.threshold:q.customTokenTh.get(s.forms[i].toLowerCase()));
				if(r[i]>=th) //XXX set th to >1.0 and nothing will be pruned.
					if(!sink.remove(sp) && !Options.NO_NOISE && false) //sick of this, added && false
						System.err.println("Failed to remove span: "+sp);
			}
		}
	}

	private NonReferentialClassifier getClassifier(String docName) {
		Integer in=docNameToIndex.get(docName);
		NonReferentialClassifier q=(in==null?test:cv[in]);
		return q;
	}

	public static int dropCount=0;
	
	
	public static void main(String[] args) throws IOException{
		Options options=new Options(args);
		Language.initLanguage(options.lang);
		DocumentReader trainReader=ReaderWriterFactory.getInputReader(options);
		NonReferentialPruner nrp=new NonReferentialPruner();
		nrp.train(trainReader,options.count);
		
		List<String> tokensToLookFor=new ArrayList<String>(getTokenSet());
//		tokensToLookFor=Arrays.asList("it","you");
		
		DocumentReader testReader=options.in2==null?trainReader:ReaderWriterFactory.getReader(options.inputFormat,options.in2,options.inGz,options.inputEnc);
		System.out.println("Testing: "+nrp.toString());
		System.out.println();
		evaluate(testReader,nrp,tokensToLookFor);
		System.out.println();
		System.out.println("DropCount: "+dropCount);
	}

	private static void evaluate(DocumentReader testReader, NonReferentialPruner nrp, List<String> surfaceFormsToLookFor) {
		int tp=0,fp=0,fn=0,itCount=0;
		for(Document d:testReader){
			NonReferentialClassifier nrc=(NonReferentialClassifier) nrp.getClassifier(d.docName);
			CorefSolution cs=GoldStandardChainExtractor.getGoldCorefSolution(d);
			for(Sentence s:d.sen){
				boolean[] q=nrc.classifiy(s.wholeForm, s.tags,d.genre);
				for(int i=1;i<s.wholeForm.length;++i){
					if(surfaceFormsToLookFor.contains(s.wholeForm[i].toLowerCase())){
						itCount++;
						boolean isGoldMention=cs.getSpanChainID(s.getSpan(i, i))!=null;
						boolean isPredMention=!q[i];
						if(!isPredMention)
							dropCount++;
						if(!isGoldMention && !isPredMention)
							tp++;
						else if(!isGoldMention && isPredMention)
							fn++;
						else if(isGoldMention && !isPredMention)
							fp++;
					}
				}
			}
		}
		double p=100.0*tp/(tp+fp);
		double r=100.0*tp/(tp+fn);
		double f=2*p*r/(p+r);
		System.out.print("Tokens:");
		for(String s:surfaceFormsToLookFor)
			System.out.print(" "+s);
		System.out.println();
		System.out.println("Token-count: "+itCount);
		System.out.println("TP="+tp+" FP="+fp+" FN="+fn);
		System.out.printf("Prec:\t %6.3f\n",p);
		System.out.printf("Rec: \t %6.3f\n",r);
		System.out.printf("F1:  \t %6.3f\n",f);
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder("NonReferentialPruner (");
		for(String s:test.TARGETS)
			sb.append(s).append("-").append((test.customTokenTh==null?test.threshold:test.customTokenTh.get(s))).append(",");
		return sb.append(")").toString();
	}
	

	private static Map<String, Double> getCustomTokenAnaphoricityTh(Set<String> tokens) {
		if(Options.customTokenAnaphoricityTh==null)
			return null;
		String[] q=Options.customTokenAnaphoricityTh.split(",");
		int i=0;
		Map<String,Double> m=new HashMap<String,Double>();
		for(String token:tokens){
			m.put(token, Double.parseDouble(q[i++]));
		}
		return m;
	}
	public static Set<String> getTokenSet() {
		Language l=Language.getLanguage();
		if(l instanceof English){
			return NonReferentialClassifier.DEFAULT_ENG_TARGETS;
		} else if(l instanceof Chinese){
			return NonReferentialClassifier.DEFAULT_CHI_TARGETS;
		} else if(l instanceof Arabic){
			return NonReferentialClassifier.DEFAULT_ARA_TARGETS;
		} else {
			throw new RuntimeException("!");
		}
	}
//	Testing: NonReferentialPruner (中国-0.9,你-0.75,)
//	Tokens: 中国 你
//	Token-count: 6052
//	TP=117 FP=51 FN=1094
//	Prec:	 69.643
//	Rec: 	  9.661
//	F1:  	 16.969
//
//	DropCount: 168
//	Testing: NonReferentialPruner (中国-0.75,你-0.9,)
//	Tokens: 中国 你
//	Token-count: 6052
//	TP=140 FP=38 FN=1071
//	Prec:	 78.652
//	Rec: 	 11.561
//	F1:  	 20.158
//
//	DropCount: 178
	
}
