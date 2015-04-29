package ims.util;

public class Pair<L,R> {
	
	L left;
	R right;
	
	public Pair(L left,R right){
		this.left=left;
		this.right=right;
	}
	
	public L getLeft(){
		return left;
	}
	
	public R getRight(){
		return right;
	}

}
