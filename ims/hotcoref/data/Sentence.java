package ims.hotcoref.data;

import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.symbols.ISymbolMapping;
import ims.hotcoref.symbols.SymbolTable;
import ims.util.IntPair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sentence {
	
	public final int sentenceIndex;
	
	public final String[] forms;
	public final String[] lemmas;
	public final String[] tags;
	public final String[][] feats;
	public final DepTree dt;
	public final String[] cfgCol;
	public final String[] neCol;
	public final CFGTree ct;
	public final List<NE> nes;
	public final String[] corefCol;
	public final String[] speaker;
	public final String[] wholeForm;
	public final String[] bwuv;
	
	public final String lastSpeaker;
	public final int[] quoteCount;	
	
	public final Document d;

	public SInst sInst;
	
//	private Collection<TypedDependency> stanfordDeps;
	
	
	private Map<IntPair,Span> spanMap=new HashMap<IntPair,Span>();
	
	public Sentence(int index,String[] forms,String[] tags,String[][] feats,DepTree depTree,String[] corefs,String[] speaker,String[] neCol,String[] cfgCol,String[] lemmas,Document d,String lastSpeaker){
		this.sentenceIndex=index;
		this.d=d;
		this.forms=forms;
		this.lemmas=lemmas;
		this.tags=tags;
		this.feats=feats;
		this.dt=depTree;
		this.corefCol=corefs;
		this.speaker=speaker;
		this.neCol=neCol;
		this.cfgCol=cfgCol;
		this.bwuv=new String[forms.length];
		ct=new CFGTree(cfgCol,this);
		nes=NE.getNEs(neCol, this);
		this.lastSpeaker=lastSpeaker;
		this.wholeForm=Arrays.copyOf(forms, forms.length);
		this.quoteCount=new int[forms.length];
	}
	
	public Span getSpan(int beg, int end){
		//XXX
//		IntPair ip=IntPair.intPairPool.get(beg, end);
		IntPair ip=new IntPair(beg,end);
		synchronized(spanMap){
			Span s=spanMap.get(ip);
			if(s==null){
				CFGNode cfgNode=(ct==null?null:ct.getExactNode(beg,end));
				s=new Span(this,beg,end,cfgNode);
				spanMap.put(ip,s);
				return s;
			} else {
				return s;
			}
		}
	}
	
	private static final String HYPHEN="-";
	private static final String BAR="|";
	public void clearCorefCol(){
		for(int i=1;i<corefCol.length;++i){
			corefCol[i]=HYPHEN;
		}
	}

	public void addCoref(Span s, Integer key) {
		if(s.start==s.end){ //Single token
			String c="("+key+")";
//			if(corefCol[s.start].contains(c)) //XXX remove this wehn done debugging
//				System.out.println("HERE!");
			if(corefCol[s.start].equals(HYPHEN)){
				corefCol[s.start]=c;
			} else {
				corefCol[s.start]+=BAR+c;
			}
			
		} else { //Multiple tokens
			String b="("+key;
			String e=key+")";
//			if(corefCol[s.start].contains(b) && corefCol[s.end].contains(e)) //XXX remove this too
//				System.out.println("HERE!");
			if(corefCol[s.start].equals(HYPHEN)){
				corefCol[s.start]=b;
			} else {
				corefCol[s.start]+=BAR+b;
			}
			if(corefCol[s.end].equals(HYPHEN)){
				corefCol[s.end]=e;
			} else {
				corefCol[s.end]+=BAR+e;
			}
		}
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<forms.length;++i){
			sb.append(forms[i]).append(" ");
		}
		return sb.toString();
	}
	
	public class SInst {
		public final int[] f; //Forms
		public final int[] l; //Lemmas
		public final int[] t; //Tags
		public final int[] b; //bwuv
		public final int[] fc; //first char
		public final int[] lc; //last char
		public final int[] f2c; //first 2 chars
		public final int[] l2c; //lsat 2 chars
		public final int[] d; //deprels
		
		SInst(int[] forms,int[] lemmas,int[] tags,int[] bwuv,int[] deprels,int[] firstChar,int[] lastChar,int[] firstTwoChar,int[] lastTwoChar){
			this.f=forms;
			this.l=lemmas;
			this.t=tags;
			this.b=bwuv;
			this.d=deprels;
			this.fc=firstChar;
			this.lc=lastChar;
			this.f2c=firstTwoChar;
			this.l2c=lastTwoChar;
		}
	}
	
	public void initInst(SymbolTable symTab){
		int[] f  = symTab.forms!=null   ? s2iArr(forms,symTab.forms)      : null;
		int[] l  = symTab.lemmas!=null  ? s2iArr(lemmas,symTab.lemmas)    : null;
		int[] t  = symTab.tags!=null    ? s2iArr(tags,symTab.tags)        : null;
		int[] b  = symTab.bwuv!=null    ? s2iArr(bwuv,symTab.bwuv)        : null;
		int[] d  = symTab.deprels!=null ? s2iArr(dt.lbls,symTab.deprels)  : null;
		int[] fc = symTab.chars!=null   ? getFirstCharArr(forms,symTab.chars) : null;
		int[] lc = symTab.chars!=null   ? getLastCharArr(forms,symTab.chars)  : null;
		int[] f2c= symTab.charBigrams!=null ? getFirstCharBG(forms,symTab.charBigrams) : null;
		int[] l2c= symTab.charBigrams!=null ? getLastCharBG(forms,symTab.charBigrams)  : null;
		sInst=new SInst(f,l,t,b,d,fc,lc,f2c,l2c);
	}

	private int[] getFirstCharBG(String[] forms,ISymbolMapping<String> charBigrams) {
		int[] i=new int[forms.length];
		for(int iq=0;iq<forms.length;++iq)
			i[iq]=charBigrams.lookup(forms[iq].substring(0,2));
		return i;
	}

	private int[] getLastCharBG(String[] forms,ISymbolMapping<String> charBigrams) {
		int[] i=new int[forms.length];
		for(int iq=0;iq<forms.length;++iq){
			int l=forms[iq].length();
			i[iq]=charBigrams.lookup(forms[iq].substring(l-2,l));
		}
		return i;
	}

	private int[] getLastCharArr(String[] forms, ISymbolMapping<String> chars) {
		int[] i=new int[forms.length];
		for(int iq=0;iq<forms.length;++iq){
			int l=forms[iq].length();
			i[iq]=chars.lookup(forms[iq].substring(l-1,l));
		}
		return i;
	}

	private int[] getFirstCharArr(String[] forms, ISymbolMapping<String> chars) {
		int[] i=new int[forms.length];
		for(int iq=0;iq<forms.length;++iq)
			i[iq]=chars.lookup(forms[iq].substring(0,1));
		return i;
	}

	private int[] s2iArr(String[] s, ISymbolMapping<String> ssm) {
		int[] i=new int[s.length];
		for(int iq=0;iq<s.length;++iq)
			i[iq]=ssm.lookup(s[iq]);
		return i;
	}
	
	public Map<IntPair,Span> getSpanMap(){
		return spanMap;
	}

//	public Collection<TypedDependency> getStanfordDeps() {
//		StringBuilder sb=new StringBuilder();
//		for(int i=1;i<forms.length;++i)
//			sb.append(cfgCol[i].replace("*","("+tags[i]+" "+forms[i]+")"));
//		if(stanfordDeps==null)
//			try {
//				stanfordDeps=StanfordDepConverter.CONVERTER_SINGLETON.getBasicDependencies(sb.toString());
//			} catch(IllegalArgumentException e){ //Happens for e.g. NOPARSE
//				return null;
//			}
//		return stanfordDeps;
//	}

}
