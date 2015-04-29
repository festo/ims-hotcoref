package ims.hotcoref.mentiongraph;

import ims.hotcoref.data.Instance;

import java.util.Collections;
import java.util.Comparator;

public class Edge {

	public final int headIdx;
	public final int depIdx; 
	
	double score;
	private int[] f;
	
	public Edge(int head,int dep){
		this.headIdx=head;
		this.depIdx=dep;
	}
	
	public void setScore(double score){
		this.score=score;
	}
	
	public double getScore(){
		return score;
	}
		
	public void setF(int[] f){
		this.f=f;
	}
	
	public int[] getF(){
		return f;
	}
	
	public INode getDep(Instance inst){
		return inst.nodes[depIdx];
	}
	
	public INode getHead(Instance inst){
		return inst.nodes[headIdx];
	}
	
	public boolean equals(Edge other){
		return headIdx==other.headIdx && depIdx==other.depIdx;
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder("e: (");
		sb.append(headIdx).append(',').append(depIdx).append(")");
		return sb.toString();
	}
	
	public int getLen(){
		return depIdx-headIdx;
	}
	
	/*
	 * Sort by edge scores. (and reverse below)
	 */
	public static final Comparator<Edge> EDGE_SCORE_COMPARATOR=new Comparator<Edge>(){
		@Override
		public int compare(Edge arg0, Edge arg1) {
			if(arg0.score<arg1.score)
				return -1;
			else if(arg0.score>arg1.score)
				return 1;
			else
				return 0;
		}
	};
	public static final Comparator<Edge> REV_EDGE_SCORE_COMPARATOR=Collections.reverseOrder(EDGE_SCORE_COMPARATOR);

}
