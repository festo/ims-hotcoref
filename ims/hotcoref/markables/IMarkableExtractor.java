package ims.hotcoref.markables;

import ims.hotcoref.data.Document;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.io.DocumentReader;

import java.io.Serializable;
import java.util.Set;

public interface IMarkableExtractor extends Serializable {

	public void extractMarkables(Sentence s,Set<Span> sink,String docName);
	public Set<Span> extractMarkables(Document d);
	public boolean needsTraining();
	public void train(DocumentReader reader, int count);
	
}
