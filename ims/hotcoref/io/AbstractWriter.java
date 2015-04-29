package ims.hotcoref.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import ims.hotcoref.data.Document;
import ims.hotcoref.data.Sentence;
import ims.util.Util;

public abstract class AbstractWriter implements DocumentWriter {

	abstract String sentenceToString(Sentence s);
	
	BufferedWriter out;
	
	public AbstractWriter(File out) throws IOException{
		this.out=Util.getWriter(out);
	}
	
	@Override
	public void write(Document d) throws IOException {
		out.write(d.header);
		out.newLine();
		for(Sentence s:d.sen){
			out.write(sentenceToString(s));
			out.newLine();
		}
		out.write(d.footer);
		out.newLine();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

}
