package ims.hotcoref.headrules;

import java.util.List;
import java.util.regex.Pattern;

public class HeadRules {

	public final String phraseLabel;
	public final Rule[] rules;
	
	public HeadRules(String phraseLabel,List<Rule> rules){
		this.phraseLabel=phraseLabel;
		this.rules=rules.toArray(new Rule[rules.size()]);
	}
	
	public HeadRules(String phraseLabel,Rule... rules){
		this.phraseLabel=phraseLabel;
		this.rules=rules;
	}
	
	public static class Rule {
		public final Direction d;
		public final Pattern headPOSPattern;
		public Rule(Direction d,Pattern headPOSPattern){
			this.d=d;
			this.headPOSPattern=headPOSPattern;
		}
	}
	
	public static enum Direction {
		LeftToRight,
		RightToleft
	}
}
