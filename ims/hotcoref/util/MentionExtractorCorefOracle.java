package ims.hotcoref.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ims.hotcoref.Options;
import ims.hotcoref.data.Chain;
import ims.hotcoref.data.CorefSolution;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Span;
import ims.hotcoref.io.DocumentReader;
import ims.hotcoref.io.DocumentWriter;
import ims.hotcoref.io.ReaderWriterFactory;
import ims.hotcoref.lang.Language;
import ims.hotcoref.markables.GoldStandardChainExtractor;
import ims.hotcoref.markables.IMarkableExtractor;
import ims.hotcoref.markables.MarkableExtractorFactory;
import ims.util.DBO;

public class MentionExtractorCorefOracle {

	public static void main(String[] args) throws IOException{
		Options options=new  Options(args);
		Language.initLanguage(options.lang);
		WordNetInterface.theInstance();
		IMarkableExtractor me=MarkableExtractorFactory.getExtractorS(options);
		DocumentReader reader=ReaderWriterFactory.getInputReader(options);
		if(me.needsTraining())
			me.train(reader,options.count);
		DBO.println("Using markable extractors: "+me.toString());
		final DocumentReader testReader;
		DocumentWriter writer=ReaderWriterFactory.getOutputWriter(options);
		if(options.in2!=null)
			testReader=ReaderWriterFactory.getReader(options.inputFormat, options.in2, options.inGz, options.inputEnc);
		else
			testReader=reader;
		doIt(testReader,writer,me);
		writer.close();
		options.done();
	}

	private static void doIt(DocumentReader reader, DocumentWriter writer,IMarkableExtractor me) throws IOException {
		DBO.println("Testing oracle");
		DBO.printWithPrefix("Doc: ");
		int erase=0;
		int docCount=0;
		for(Document d:reader){
			docCount++;
			erase=DBO.eraseAndPrint(erase, Integer.toString(docCount));
			CorefSolution cs=GoldStandardChainExtractor.getGoldCorefSolution(d);
//			CorefSolution pred=new CorefSolution();
			Set<Span> extracted=me.extractMarkables(d);
			Map<Integer,Chain> m=new HashMap<Integer,Chain>();
			Map<Span,Integer> gold=cs.getSpan2IntMap();
			for(Span s:extracted){
				Integer cid=gold.get(s);
				if(cid!=null){
					Chain c=m.get(cid);
					if(c!=null)
						c.addSpan(s);
					else {
						c=new Chain(cid,s);
						m.put(cid, c);
					}
				}
			}
			CorefSolution pred=new CorefSolution(m);
			d.clearCorefCols();
			d.setCorefCols(pred.getKey());
			writer.write(d);
		}
		DBO.println();
		DBO.println();
	}
	
}
