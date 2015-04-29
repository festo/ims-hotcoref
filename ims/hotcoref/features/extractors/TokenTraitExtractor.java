package ims.hotcoref.features.extractors;

import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.mentiongraph.INode;
import ims.hotcoref.mentiongraph.MNode;
import ims.hotcoref.mentiongraph.VNode;
import ims.hotcoref.symbols.AbstractSymbolMapping;
import ims.hotcoref.symbols.ISymbolMapping;
import ims.hotcoref.symbols.SymbolTable;

import java.io.Serializable;

public abstract class TokenTraitExtractor implements Serializable{
	private static final long serialVersionUID = 1L;

	public final TokenTrait tt;
	
	private TokenTraitExtractor(TokenTrait tt){
		this.tt=tt;
	}
	
//	@Deprecated
//	public String getTrait(Sentence s,int index){
//		if(index<0 || index>=s.forms.length)
//			return "<no-token>";
//		switch(tt){
//		case Form:  return s.forms[index];
//		case Pos:   return s.tags[index];
//		case Fun:   return s.dt.lbls[index];
//		case Lemma: return s.lemmas[index];
//		case BWUV: return s.bwuv[index];
//		case FFChar:return s.forms[index].length()<2?null:s.forms[index].substring(0, 1);
//		case FLChar:return s.forms[index].length()<2?null:s.forms[index].substring(s.forms[index].length()-1);
//		case FF2Char:return s.forms[index].length()<3?null:s.forms[index].substring(0,2);
//		case FL2Char:return s.forms[index].length()<3?null:s.forms[index].substring(s.forms[index].length()-2);
//		default: throw new Error("!");
//		}
//	}
	public abstract String getTrait(Sentence s,int index);
	public abstract ISymbolMapping<String> getTraitMapping(SymbolTable symTab);
	public abstract int[] getArr(Sentence s);
	
	public int getBitsES(SymbolTable symTab){
		return symTab.esTrait[tt.ordinal()].getBits();
	}
	
	public int getBitsEST(SymbolTable symTab){
		return symTab.esTTrait[tt.ordinal()].getBits();
	}
	
	
	public static TokenTraitExtractor getTTE(TokenTrait tt){
		switch(tt){
		case Form:		return new FormTTE(tt);
		case Pos:		return new PosTTE(tt);
		case Lemma:		return new LemmaTTE(tt);
		case BWUV:		return new BWUVTTE(tt);
		case FLChar:	return new FLChar(tt);
		default: throw new Error("not implemented: "+tt.toString());
		}
	}
	
	
	public int getBits(SymbolTable symTab){
		return getTraitMapping(symTab).getBits();
	}
	public int getBitsWS(SymbolTable symTab){
		return getWSMapping(symTab).getBits();
	}
	
	


	public ISymbolMapping<char[]> getWSMapping(SymbolTable symTab) {
		return symTab.wsTrait[tt.ordinal()];
	}
	
	
	public int computeIntValue(INode node,SpanTokenExtractor ste,SymbolTable symTab){
		if(node instanceof VNode)
			return AbstractSymbolMapping.getVNodeIntVal((VNode) node);
		else
			return computeIntValue(((MNode) node).span,ste);
	}
	
	public int computeIntValue(Span span,SpanTokenExtractor ste){
		int idx=ste.getToken(span);
		int[] a=getArr(span.s);
		if(idx==-1 || idx>=a.length)	return AbstractSymbolMapping.NONE_IDX;
		else		return a[idx];
	}
	
	public int lookupWS(SymbolTable symTab,Span span) {
		char[] sym=new char[span.size()];
		int[] a=getArr(span.s);
		for(int i=span.start,j=0;i<=span.end;++i,++j){
			int q=a[i];
			if(q==AbstractSymbolMapping.UNK_IDX)
				return AbstractSymbolMapping.UNK_IDX;
			sym[j]=(char) q;
		}
		ISymbolMapping<char[]> wsMap=getWSMapping(symTab);
		return wsMap.lookup(sym);
	}
	
//	public abstract int computeIntValue(Span span,SpanTokenExtractor ste,SymbolTable symTab);
	

	
	
	static class FormTTE extends TokenTraitExtractor {
		private static final long serialVersionUID = 1598540659246887869L;
		FormTTE(TokenTrait tt){	super(tt); }
		@Override
		public
		int[] getArr(Sentence s) {
			return s.sInst.f;
		}
		@Override
		public
		ISymbolMapping<String> getTraitMapping(SymbolTable symTab) {
			return symTab.forms;
		}
		@Override
		public String getTrait(Sentence s, int index) {
			return s.forms[index];
		}
	}
	static class PosTTE extends TokenTraitExtractor {
		private static final long serialVersionUID = 5004522020055722235L;
		PosTTE(TokenTrait tt) { super(tt); }
		@Override
		public
		int[] getArr(Sentence s) {
			return s.sInst.t;
		}
		@Override
		public
		ISymbolMapping<String> getTraitMapping(SymbolTable symTab) {
			return symTab.tags;
		}
		@Override
		public String getTrait(Sentence s, int index) {
			return s.tags[index];
		}
	}
	static class LemmaTTE extends TokenTraitExtractor {
		private static final long serialVersionUID = 1L;
		LemmaTTE(TokenTrait tt) { super(tt); }
		@Override
		public int[] getArr(Sentence s) {
			return s.sInst.l;
		}
		@Override
		public ISymbolMapping<String> getTraitMapping(SymbolTable symTab) {
			return symTab.lemmas;
		}
		@Override
		public String getTrait(Sentence s, int index) {
			return s.lemmas[index];
		}
	}
	static class BWUVTTE extends TokenTraitExtractor {
		private static final long serialVersionUID = 1097369621473107624L;
		BWUVTTE(TokenTrait tt) { super (tt);  }
		@Override
		public String getTrait(Sentence s, int index) {
			return s.bwuv[index];
		}
		@Override
		public int[] getArr(Sentence s) {
			return s.sInst.b;
		}
		@Override
		public ISymbolMapping<String> getTraitMapping(SymbolTable symTab) {
			return symTab.bwuv;
		}
	}
	static class FLChar extends TokenTraitExtractor {
		private static final long serialVersionUID = -1366303695187095330L;
		FLChar(TokenTrait tt) { super(tt); }
		@Override
		public String getTrait(Sentence s, int index) {
			String st=s.forms[index];
			int l=st.length();
			return st.substring(l-1, l);
		}
		@Override
		public int[] getArr(Sentence s) {
			return s.sInst.lc;
		}
		@Override
		public ISymbolMapping<String> getTraitMapping(SymbolTable symTab) {
			return symTab.chars;
		}
	}

}
