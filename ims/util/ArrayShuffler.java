package ims.util;

import java.util.Random;

public class ArrayShuffler {

	private final Random r;
	
	private static final long DEFAULT_SEED=1357900361l;
	
	public ArrayShuffler(){
		this(DEFAULT_SEED);
	}
	
	public ArrayShuffler(long seed){
		this(new Random(seed));
	}
	
	public ArrayShuffler(Random r){
		this.r=r;
	}
	
	public synchronized <T> void shuffle(T[] arr){
		for(int i=arr.length-1;i>=0;--i){
			int swapWith=r.nextInt(i+1);
			T tmp=arr[swapWith];
			arr[swapWith]=arr[i];
			arr[i]=tmp;
		}
	}
	
	public static <T> void shuffleArray(T[] arr){
		ArrayShuffler s=new ArrayShuffler();
		s.shuffle(arr);
	}
}
