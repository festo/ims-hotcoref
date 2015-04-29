package ims.util;

public class IntPair {

	public static IntPairPool intPairPool=new IntPairPool();
	
	public final int i1;
	public final int i2;
	
	public IntPair(int i,int j){
		this.i1=i;
		this.i2=j;
	}
	
	public boolean equals(Object other){
		if(other instanceof IntPair)
			return equals((IntPair) other);
		else
			return false;
	}
	public boolean equals(IntPair other){
		return other.i1==i1 && other.i2==i2;
	}
	
	public int hashCode(){
		return 31*i1+2*i2;
	}
	
	public String toString(){
		return "i1: "+i1+", i2: "+i2;
	}
	
	public static class IntPairPool {
		private static final int INIT_SIZE=100;
		private IntPair[][] ips;
		private IntPairPool(){
			ips=new IntPair[INIT_SIZE][INIT_SIZE];
		}

		public final synchronized IntPair get(int i,int j){
			if(i>=ips.length)
				resizeI(i);
			if(j>=ips[i].length)
				resizeJ(i,j);
			if(ips[i][j]==null)
				ips[i][j]=new IntPair(i,j);
			return ips[i][j];
		}
		private final void resizeI(int i) {
			IntPair[][] q=new IntPair[i+1][];
			System.arraycopy(ips, 0, q, 0, ips.length);
			for(int k=ips.length;k<q.length;++k)
				q[k]=new IntPair[INIT_SIZE];
			ips=q;
		}
		private final void resizeJ(int i,int j) {
			IntPair[] q=new IntPair[j+1];
			System.arraycopy(ips[i], 0, q, 0, ips[i].length);
			ips[i]=q;
		}
	}
	
//	public static Comparator<IntPair> IP_CMP_EMBEDDING=new Comparator<IntPair>(){
//		//With pairs:
//		//(0,1) , (2,3) , (15,16) , (8,14) , (8,10) , (12,14)
//		//(0,1) , (2,3) , (8,14) , (8,10) , (12,14) , (15,16)
//		
//		@Override
//		public int compare(IntPair arg0, IntPair arg1) {
//			if(arg0.i1<arg1.i1)
//				return -1;
//			if(arg0.i1>arg1.i1)
//				return 1;
//			if(arg0.i2<arg1.i2)
//				return -1;
//			if(arg0.i2>arg1.i2)
//				return 1;
//			else
//				throw new Error("two identical int pairs -- sure you want to compare them?");
//		}
//	};

}
