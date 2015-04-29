package ims.hotcoref.markables;

import ims.hotcoref.data.NE;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;

import java.util.Set;
import java.util.regex.Pattern;

public abstract class AbstractNERExtractor extends AbstractMarkableExtractor {
	private static final long serialVersionUID = 1L;

	@Override
	public void extractMarkables(Sentence s, Set<Span> sink,String docName) {
		for(NE ne:s.nes){
			if(take(ne.getLabel()))
				sink.add(s.getSpan(ne.b, ne.e));
		}
	}
	
	abstract boolean take(String lbl);

	public static class AllNERExtractor extends AbstractNERExtractor {
		private static final long serialVersionUID = 1L;

		@Override
		boolean take(String lbl) {
			return true;
		}

		public String toString(){
			return "NER-ALL";
		}
	}
	
	public static class OneNERExtractor extends AbstractNERExtractor {
		private static final long serialVersionUID = 1L;

		public final String label;
		
		public OneNERExtractor(String label){
			this.label=label;
		}
		
		@Override
		boolean take(String lbl) {
			return lbl.equals(label);
		}
		
		public String toString(){
			return "NER-"+label;
		}
	}
	
	public static class MultipleNERExtractor extends AbstractNERExtractor {
		private static final long serialVersionUID = 1L;
		
		private final Pattern pattern;
		
		public MultipleNERExtractor(String... labels){
			StringBuilder sb=new StringBuilder("(?:"+labels[0]);
			for(int i=1;i<labels.length;++i)
				sb.append('|').append(labels[i]);
			sb.append(')');
			pattern=Pattern.compile(sb.toString());
		}
		
		@Override
		boolean take(String lbl) {
			return pattern.matcher(lbl).matches();
		}
		
	}
	
}
