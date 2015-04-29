package ims.hotcoref.data;

import ims.hotcoref.Options;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.features.enums.Gender;
import ims.hotcoref.features.enums.Num;
import ims.hotcoref.features.enums.SemanticClass;
import ims.hotcoref.lang.Language;
import ims.hotcoref.mentiongraph.MNode;

public class Span implements Comparable<Span> {

	public final Sentence s;
	public final int start;
	public final int end;
	public final NE ne;
	
	public int hd=-1;
	public int hdgov=-1;
	public int hdlmc=-1;
	public int hdrmc=-1;
	public int hdls=-1;
	public int hdrs=-1;
	
	public CFGTree.CFGNode cfgNode;
	
	//Atomic span features below:
	public boolean isProperName;
	public boolean isPronoun;
	public boolean isDefinite;
	public boolean isDemonstrative;
	public boolean isQuoted;
	
	public Gender gender=Gender.Unknown;
	public Num number=Num.Unknown;
	public SemanticClass semanticClass=SemanticClass.Unknown;
	
	
	public double anaphoricityPr=-1;
	
	Span(Sentence s,int st,int e,CFGNode cfgNode){
		this.s=s;
		this.start=st;
		this.end=e;
		this.cfgNode=cfgNode;
		assignHeadsEtc();
		ne=getNamedEntity();
		Language.getLanguage().computeAtomicSpanFeatures(this);
	}
	
	//XXX
	//Maybe we should also consider spans embedding an NE, or an NE embedding a span... not sure. Look at this later.
	private NE getNamedEntity() {
		for(NE ne:s.nes){
			if(ne.b==start && ne.e==end)
				return ne;
		}
		return null;
	}

	private void assignHeadsEtc() {
		//assing head
		if(start==end)
			hd=start;
		else {
			switch(Options.headFinding){
			case DepTree:
				hd=end;
				int newHd=s.dt.heads[hd];
				while(newHd<=end && newHd>=start){
					hd=newHd;
					newHd=s.dt.heads[hd];
				}
				break;
			case Rules:
				CFGNode n=s.ct.getExactNode(start, end);
				hd=(n==null?-1:n.getHead());
				if(hd>end || hd<start)
					hd=end;
				break;
			default:
				throw new Error("not implemented");
			}
		}
		if(s.dt==null)
			return;
		//Head Gov
		hdgov=s.dt.heads[hd];
		//Head Lmc
		for(int i=1;i<hd;++i){
			if(s.dt.heads[i]==hd){
				hdlmc=i;
				break;
			}
		}
		//Head Rmc
		for(int i=s.forms.length-1;i>hd;--i){
			if(s.dt.heads[i]==hd){
				hdrmc=i;
				break;
			}
		}
		//Hd ls
		for(int i=hd-1;i>0;--i){
			if(s.dt.heads[i]==s.dt.heads[hd]){
				hdls=i;
				break;
			}
		}
		//Hd rs
		for(int i=hd+1;i<s.forms.length;++i){
			if(s.dt.heads[i]==s.dt.heads[hd]){
				hdrs=i;
				break;
			}
		}
	}

	public int hashCode(){
		return 19*start+31*end+2*s.sentenceIndex;
	}
	
	public boolean equals(Object other){
		if(other instanceof Span)
			return equals((Span) other);
		else
			return false;
	}
	
	public boolean equals(Span other){
		if(other==this)
			return true;
		else if(other.s==s && other.start==start && other.end==end)
			return true;
//			throw new Error("! -- two identical spans as separate objects. This shouldn't happend.");
		else
			return false;
	}

	@Override
	public int compareTo(Span other) {
		if(equals(other))
			return 0;
		int senA=s.sentenceIndex;
		int senB=other.s.sentenceIndex;
		if(senA<senB){
			return -1;
		} else if(senB<senA){
			return 1;
		}
		int begA=start;
		int begB=other.start;
		if(begA<begB){
			return -1;
		} else if(begB<begA){
			return 1;
		}
		int endA=end;
		int endB=other.end;
		if(endA<endB){
			return 1;
		} else {
			return -1;
		}
	}
	
	public String getKey(){
		String key=s.sentenceIndex+"-"+start+"-"+end;
		return key;
	}

	public int size() {
		return end-start+1;
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		for(int i=start;i<=end;++i){
			sb.append(s.forms[i]).append(" ");
		}
		sb.append('\n');
		if(hd!=1) sb.append("Hd\t"+hd+"\t"+s.forms[hd]).append('\n');
		if(hdgov!=-1) sb.append("Hdgov\t"+hdgov+"\t"+s.forms[hdgov]).append('\n');
		if(hdrmc!=-1) sb.append("HdRmc\t"+hdrmc+"\t"+(hdrmc==-1?"-1":s.forms[hdrmc])).append('\n');
		if(hdlmc!=-1) sb.append("HdLmc\t"+hdlmc+"\t"+(hdlmc==-1?"-1":s.forms[hdlmc])).append('\n');
		if(hdrs!=-1)  sb.append("HdRs\t"+hdrs+"\t"+(hdrs==-1?"-1":s.forms[hdrs])).append('\n');
		if(hdls!=-1)  sb.append("HdLs\t"+hdls+"\t"+(hdls==-1?"-1":s.forms[hdls])).append('\n');
		sb.append("NE\t"+"\t"+(ne==null?"null":ne.getLabel()));
		return sb.toString();
	}
	
	public boolean embeds(Span other){
		return  s==other.s && other.start>=start && other.end<=end;
	}
	
	public boolean isEmbeddedIn(Span other){
		return other.embeds(this);
	}
	
	static final int MAX_KEY_PART=(1<<10);
	static final int MAX_KEY_PART_M1=MAX_KEY_PART-1;
	public int getUniqueIntKey(){
		//we have 31 bits to play with, so lets give each number 10 bits.
		//Basically this contrains the number of sentences per document and the number of tokens per sentence to 1023
		if(s.sentenceIndex>MAX_KEY_PART || end>MAX_KEY_PART)
			throw new Error("Sentence index or token index to big!");
		int key=s.sentenceIndex;
		key<<=10;
		key|=start;
		key<<=10;
		key|=end;
		return key;
	}

	private MNode graphNode;
	public MNode getGraphNode() {
		if(graphNode==null)
			graphNode=new MNode(this);
		return graphNode;
	}

	public String getSurfaceForm() {
		StringBuilder sb=new StringBuilder();
		for(int i=start;i<=end;++i)
			sb.append(s.forms[i]).append(' ');
		return sb.toString();
	}
	
	private String cleverString;
	public String getCleverString(){
		if(cleverString==null)
			cleverString=Language.getLanguage().computeCleverString(this);
		return cleverString;
	}
}
