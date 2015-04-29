package ims.hotcoref.data;

public class DepTree {

	public final int[] heads;
	public final String[] lbls;
	public final boolean[][] isChild; //This is indexed as isChild[anc][dep]
	
	public DepTree(int[] heads,String[] lbls){
		this.heads=heads;
		this.lbls=lbls;
		this.isChild=new boolean[heads.length][heads.length];
        for(int i = 1, l1=1; i < heads.length; i++,l1=i)  
            while((l1= heads[l1]) != -1) isChild[l1][i] = true;
	}

	public Span getSubTreeSpan(int head,Sentence s) {
		int beg=head;
		int end=head;
		for(int r=1;r<head;++r){
			if(isChild[head][r]){
				beg=r;
				break;
			}
		}
		for(int t=s.forms.length-1;t>head;--t){
			if(isChild[head][t]){
				end=t;
				break;
			}
		}
		Span span=s.getSpan(beg, end);
		return span;
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<heads.length;++i){
			sb.append(Integer.toString(heads[i])).append('\t').append(lbls[i]).append('\n');
		}
		return sb.toString();
	}
	
	public int getRightMostChild(int head){
		int rmc=-1;
		for(int i=heads.length-1;i>head;--i){
			if(heads[i]==head){
				rmc=i;
				break;
			}
		}
		return rmc;
	}
	
	public int getLeftMostChild(int head){
		int lmc=-1;
		for(int i=1;i<head;++i){
			if(heads[i]==head){
				lmc=i;
				break;
			}
		}
		return lmc;
	}

	public int getLeftMostDep(int head) {
		int lmd=-1;
		for(int i=head-1;i>0;--i){
			if(isChild[head][i]){
				lmd=i;
			}
		}
		return lmd;
	}
}
