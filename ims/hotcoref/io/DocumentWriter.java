package ims.hotcoref.io;

import java.io.IOException;

import ims.hotcoref.data.Document;

public interface DocumentWriter {

	public void write(Document d) throws IOException;
	public void close() throws IOException;
	
}
