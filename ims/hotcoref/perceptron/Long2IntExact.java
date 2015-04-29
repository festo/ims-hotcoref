package ims.hotcoref.perceptron;

import ims.util.DBO;

/**
 * @author Bernd Bohnet, 01.09.2009
 * 
 * Maps for the Hash Kernel the long values to the int values.
 */
final public class Long2IntExact implements Long2IntInterface {
	private static final long serialVersionUID = -7391012080162670894L;

	private final gnu.trove.TLongIntHashMap mapt = new gnu.trove.TLongIntHashMap();
	
	private int cnt=0;
	private final int maxSize;
	private boolean frozen=false;
	
	public Long2IntExact(int maxSize){
		this.maxSize=maxSize;
	}
	
	public int size() {
		return maxSize;
	}

	
		
	public int l2i(long l) {		
		if (l<0) return -1;
		
		if(mapt.containsKey(l))
			return mapt.get(l);

		if(frozen)
			return -1;
		if(cnt==maxSize-1)
			throw new Error("hit the maximum number of features. restart with greater hash size");
		
		int r=cnt++;
		mapt.put(l, r);
		return r;
	}
	
	public int getNext(){
		return cnt;
	}

	@Override
	public void freeze() {
		if(!frozen){
			DBO.println("Froze l2i at "+cnt);
			frozen=true;
		}
	}
	

	@Override
	public void unFreeze() {
		if(frozen){
			DBO.println("Unfroze l2i at "+cnt);
			frozen=false;
		}
	}

	@Override
	public boolean frozen() {
		return frozen;
	}
}
