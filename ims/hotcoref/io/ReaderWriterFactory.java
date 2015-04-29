package ims.hotcoref.io;

import ims.hotcoref.Options;
import ims.hotcoref.Options.Format;

import java.io.File;
import java.io.IOException;

public class ReaderWriterFactory {

	public static DocumentReader getInputReader(Options options) throws IOException{
		return getReader(options.inputFormat,options.in,options.inGz,options.inputEnc);
	}
	public static DocumentReader getReader(Format format,File input,boolean gzipped,String enc) throws IOException{
		switch(format){
		case C12:		return new C12Reader(input,enc,gzipped);
		case C12AUG:	return new C12AugReader(input,enc,gzipped);
		default:
		}
		throw new Error("Not implemented");
	}
	
	public static DocumentWriter getOutputWriter(Options options) throws IOException{
		return getWriter(options.outputFormat,options.out,options.outGz,options.outputEnc);
	}
	public static DocumentWriter getWriter(Format format,File output,boolean gzipped,String enc) throws IOException{
		switch(format){
		case C12:		return new C12Writer(output);
		case HTML:		return new HTMLWriter(output);
		default:
		}
		throw new Error("Not implemented");
	}
	
}
