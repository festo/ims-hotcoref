package ims.hotcoref.markables;

import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;

import java.util.Set;
import java.util.regex.Pattern;

public class StandAloneDemonstrativeDepExtractor extends AbstractMarkableExtractor{
	private static final long serialVersionUID = 8115797133633986449L;

	/*
	 * Two ways of doing this are conceivable.
	 * The basic assumption is that when This or That doesn't modify a noun, they are standalone nps.
	 * I *think* this is equivalent to them being attached to verbs. Add to this potential parser noise.
	 * 
	 * In the end one can either extract
	 * 1) This/That when they modify anything _not_ a noun
	 * 2) This/That when they modify a verb
	 * 
	 * Trial and error on train set will reveal the correct solution.
	 */

	private static final Pattern THIS_THAT_PATTERN=Pattern.compile("^th(?:is|at)$", Pattern.CASE_INSENSITIVE); 
	
	@Override
	public void extractMarkables(Sentence s, Set<Span> sink, String docName) {
		for(int i=1;i<s.forms.length;++i){
			if(THIS_THAT_PATTERN.matcher(s.forms[i]).matches()){
				String headTag=s.tags[s.dt.heads[i]];
				//The two cases from above:
				if(headTag.startsWith("V")){
					sink.add(s.getSpan(i, i));
				}
//				if(!headTag.startsWith("N")){
//					sink.add(s.getSpan(i, i));
//				}
			}
		}
	}

}
