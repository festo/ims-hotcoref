package ims.hotcoref.decoder;

import java.util.Comparator;

import ims.hotcoref.data.Instance;
import ims.hotcoref.mentiongraph.Edge;
import ims.hotcoref.mentiongraph.INode;

public class HOTEdge {
	Edge e;
	int[] hotFeatures;
	double hotScore;
	
	public HOTEdge(Edge e,int[] hotFeatures,double hotScore){
		this.e=e;
		this.hotFeatures=hotFeatures;
		this.hotScore=hotScore;
	}
	
	public int getLen(){
		return e.depIdx-e.headIdx;
	}
	
	public int getHeadIdx(){
		return e.headIdx;
	}
	
	public int getDepIdx(){
		return e.depIdx;
	}
	
	public INode getHead(Instance inst){
		return e.getHead(inst);
	}

	public INode getDep(Instance inst){
		return e.getDep(inst);
	}
	
	public double score(){
		return hotScore+e.getScore();
	}

	public int[] getFO() {
		return e.getF();
	}
	
	public int[] getHO() {
		return hotFeatures;
	}
	
	public static final Comparator<? super HOTEdge> HOTEDGE_DEP_COMPARATOR=new Comparator<HOTEdge>(){
		@Override
		public int compare(HOTEdge arg0, HOTEdge arg1) {
			return arg0.getDepIdx()-arg1.getDepIdx();
		}
	};
	
	public String toString(){
		return e.toString();
	}
	
}
