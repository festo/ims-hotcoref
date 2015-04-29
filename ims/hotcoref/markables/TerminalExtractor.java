package ims.hotcoref.markables;

import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;

import java.util.Set;

public class TerminalExtractor extends AbstractMarkableExtractor{
	private static final long serialVersionUID = 1L;

	final String tag;
	
	public TerminalExtractor(String tag){
		this.tag=tag;
	}
	
	@Override
	public void extractMarkables(Sentence s, Set<Span> sink,String docName) {
		for(int i=1;i<s.tags.length;++i){
			if(s.tags[i].equals(tag))
				sink.add(s.getSpan(i, i));
		}
	}

	public String toString(){
		return "T-"+tag;
	}
}
