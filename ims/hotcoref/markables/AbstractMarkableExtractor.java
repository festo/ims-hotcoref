package ims.hotcoref.markables;

import ims.hotcoref.data.Document;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.io.DocumentReader;

import java.util.Set;
import java.util.TreeSet;

public abstract class AbstractMarkableExtractor implements IMarkableExtractor {
	private static final long serialVersionUID = 1L;

	@Override
	public Set<Span> extractMarkables(Document d) {
		Set<Span> sink=new TreeSet<Span>();
		for(Sentence s:d.sen){
			Set<Span> s2=new TreeSet<Span>();
			extractMarkables(s,s2,d.docName);
			sink.addAll(s2);
		}
		return sink;
	}

	@Override
	public boolean needsTraining() {
		return false;
	}

	@Override
	public void train(DocumentReader reader,int count) {
		throw new Error("you are wrong here");
	}

}
