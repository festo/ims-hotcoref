package ims.hotcoref;

import java.io.File;
import java.io.IOException;

import ims.hotcoref.Options.Format;
import ims.hotcoref.data.Document;
import ims.hotcoref.io.DocumentReader;
import ims.hotcoref.io.DocumentWriter;
import ims.hotcoref.io.ReaderWriterFactory;
import ims.hotcoref.lang.Language;
import ims.hotcoref.util.WordNetInterface;
import ims.util.DBO;

public class Gold2HTML {

	public static void main(String[] args) throws IOException{
		Options options=new Options(args);
		WordNetInterface.theInstance();
		Language.initLanguage(options.lang);
		DocumentReader reader=ReaderWriterFactory.getInputReader(options);
		int docCount=0;
		DBO.println("Reading gold: "+options.in.toString());
		DBO.println("Printing graphs to: "+ options.graphOutDir);
		DBO.printWithPrefix("Doc: ");
		int erase=0;
		for(Document d:reader){
			docCount++;
			erase=DBO.eraseAndPrint(erase, Integer.toString(docCount));
			String filename=docCount+"."+options.fileOutInfix+".html";
			File outFile=new File(options.graphOutDir,filename);
			DocumentWriter writer=ReaderWriterFactory.getWriter(Format.HTML, outFile, false, "UTF8");
			writer.write(d);
			writer.close();
		}
		DBO.println();
		options.done();
	}
}
