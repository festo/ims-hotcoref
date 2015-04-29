package ims.hotcoref.lang;

import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.features.FeatureSet;

import java.io.Serializable;
import java.util.Set;

public abstract class Language implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String ZIP_ENTRY = "_lang";
	public static final String ZIP_TXT_ENTRY = "_lang.txt";

	public abstract FeatureSet getDefaultFeatureSet();	
	public abstract boolean cleverStringMatch(Span ant,Span ana);
	public abstract boolean isAlias(Span ant,Span ana);
	public abstract void computeAtomicSpanFeatures(Span s);
	public abstract int findNonTerminalHead(Sentence s,CFGNode node);
	public abstract String getDefaultMarkableExtractors();
	public abstract String computeCleverString(Span span);
	public abstract String getDefaultEdgeCreators();
	
	static Language lang;
	
	protected Language(){
		System.out.println("Initializing language: "+this.getClass().getCanonicalName());
	}
	
	public static Language initLanguage(String lang){
		lang=lang.toLowerCase();
		if(lang.startsWith("eng")){
			Language.lang=new English();
		} else if(lang.startsWith("chi")) {
			Language.lang=new Chinese();
		} else if(lang.startsWith("ara")) {
			Language.lang=new Arabic();
		} else {
			throw new RuntimeException("Unknown language: "+lang);
		}
		return Language.lang;
	}

	public static Language getLanguage(){
		return lang;
	}
	public static void setLanguage(Language lang) {
		Language.lang=lang;
	}

	public void preprocessSentence(Sentence s){
		//Do nothing -- but you may overload this
	}
	public Set<String> getNonReferentialTokenSet() {
		throw new Error("not implemented for this language");
	}
	
	public String getLang() {
		return this.getClass().getSimpleName().substring(0, 3).toLowerCase();
	}

	public boolean isCoordToken(String string) {
		return false;
	}
	
}