package ims.hotcoref.markables;

import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;

import java.util.Set;

public class SubTreeExtractor extends AbstractMarkableExtractor{
	private static final long serialVersionUID = 1L;
	
	public final String tag;
	
	public SubTreeExtractor(String tag){
		this.tag=tag;
	}

	@Override
	public void extractMarkables(Sentence s, Set<Span> sink,String docName) {
		for(int i=1;i<s.forms.length;++i){
			if(take(s.tags[i])){
				Span sp=s.dt.getSubTreeSpan(i,s);
				sink.add(sp);
			}
		}
	}

	boolean take(String tag){
		return tag.startsWith(this.tag);
	}

	public String toString() {
		return "ST-"+tag;
	}
}
