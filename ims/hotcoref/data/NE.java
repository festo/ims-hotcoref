package ims.hotcoref.data;

import java.util.ArrayList;
import java.util.List;

public class NE {

	public final int b;
	public final int e;
//	Sentence s;
	String label;
	
	public NE(int beg,int end,Sentence s,String label){
		b=beg;
		e=end;
//		this.s=s;
		this.label=label;
	}
	
	public String getLabel(){
		return label;
	}
	
//	public Span getSpan(){
//		return s.getSpan(b, e);
//	}
	
	public static List<NE> getNEs(String[] neCol, Sentence s) {
		List<NE> n=new ArrayList<NE>();
		String curLbl=null;
		int begIndex=-1;
		for(int i=1;i<neCol.length;++i){
			if(neCol[i].equals("*") || neCol[i].equals("-"))
				continue;
			else if(neCol[i].startsWith("(")){
				if(curLbl!=null)
					throw new RuntimeException("Nested NEs. Shouldn't happen");
				String label=neCol[i].substring(1, neCol[i].length()-1);
				if(neCol[i].endsWith(")")){
					n.add(new NE(i,i,s,label));
				} else {
					begIndex=i;
					curLbl=label;
				}
			} else if(neCol[i].equals("*)")){
				n.add(new NE(begIndex,i,s,curLbl));
				begIndex=-1;
				curLbl=null;
			} else {
				throw new RuntimeException("Failed to parse NE: "+neCol[i]);
			}
		}
		return n;
	}

	public String toString(Sentence s){
		StringBuilder sb=new StringBuilder(label).append(": '");
		for(int i=b;i<=e;++i)
			sb.append(s.forms[i]).append(' ');
		return sb.append("'").toString();
	}
}
