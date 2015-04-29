package ims.hotcoref.mentiongraph;

import java.util.Map;
import java.util.TreeMap;

import ims.hotcoref.data.Span;
import ims.hotcoref.features.enums.Gender;

public class MNode implements INode{

	public Span span;
	public final short sIdx;
	
	public MNode(Span span){
		this.span=span;
		int s=span.s.sentenceIndex;
		if(s>=Short.MAX_VALUE)
			throw new Error("too many sentences, can't fit in short. Tough luck!");
		this.sIdx=(short)s;
	}
	
	public int hashCode(){
		if(span!=null)
			return span.hashCode();
		else
			return super.hashCode();
	}
	
	public boolean equals(Object other){
		if(!(other instanceof MNode)) 
			return false;
		if(this.span==null)
			return super.equals(other);
		else
			return ((MNode)other).span.equals(this.span);
	}

	@Override
	public boolean isVirtualNode() {
		return false;
	}

	@Override
	public String getKey() {
		return span.getKey();
	}

	@Override
	public String getDotNodeIdentifier(){
		if(span==null)
			throw new Error("need to retain spans here");
		return span.s.sentenceIndex+"-"+span.start+"-"+span.end;
	}
	
	public Map<String,String> getNodeAttributeList(){
		Map<String,String> r=new TreeMap<String,String>();
		r.put("Gender", span.gender.toString());
		r.put("Number", span.number.toString());
		String type="Common";
		if(span.isPronoun) type="Pronoun"; else if(span.isProperName) type="Name";
		r.put("Type", type);
		r.put("HEAD", Integer.toString(span.hd));
//		if(Language.getLanguage().getLang().equals("eng")){
//			//get Entity-Grid label, use stanford conversion
//			Collection<TypedDependency> tds=span.s.getStanfordDeps();
//			String egLabel="X";
//			if(tds!=null){
//				for(TypedDependency td:tds){
//					if(td.dep().index()==span.hd){
//						egLabel=StanfordDepConverter.getEGLabel(td);
//						break;
//					}
//				}
//			}
//			r.put("GF", egLabel);
//		}
		return r;
	}
	
	@Override
	public String getDotNodeName(int nodeIdx) {
		if(span==null)
			return Integer.toString(nodeIdx);
		StringBuilder sb=new StringBuilder(span.getSurfaceForm().replaceAll("\"", "\\\\\"")).append("\\n");
		sb.append(span.getKey());
		sb.append("\\n");
		String g=span.gender.toString().substring(0,2);
		String n=span.number.toString().substring(0, 2);
		String t="Com";
		if(span.isPronoun) t="Pro"; else if(span.isProperName) t="Name";
		sb.append("G:"+g+", N:"+n+", T:"+t);
		return sb.toString();
	}
	
	public int compareTo(INode arg0) {
		if(arg0 instanceof MNode)
			return compareTo((MNode) arg0);
		else
			return INode.INODE_COMPARATOR.compare(this, arg0);
	}
	
	public int compareTo(MNode arg0) {
		return this.span.compareTo(arg0.span);
	}

	@Override
	public Gender getGender() {
		return span.gender;
	}
	
	public String toString(){
		return span.toString();
	}

}
