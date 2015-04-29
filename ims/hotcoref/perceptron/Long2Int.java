package ims.hotcoref.perceptron;



/**
 * @author Bernd Bohnet, 01.09.2009
 * 
 * Maps for the Hash Kernel the long values to the int values.
 */
final public class Long2Int implements Long2IntInterface {
	private static final long serialVersionUID = 4548073026296745572L;


	public Long2Int() {
		size=115911564;
	}
	
	
	public Long2Int(int s) {
		size=s;
	}
	
	
	/** Integer counter for long2int */
	final private int size; //0x03ffffff //0x07ffffff
	                       
				
	public  int size() {return size;}
		
	final public int l2i(long l) {		
		if (l<0) return -1;
		long r= l;// 27
		l = (l>>13)&0xffffffffffffe000L;
		r ^= l;   // 40
		l = (l>>11)&0xffffffffffff0000L;
		r ^= l;   // 51
		l = (l>>9)& 0xfffffffffffc0000L; //53
		r ^= l;  // 60
		l = (l>>7)& 0xfffffffffff00000L; //62
		r ^=l;    //67
		int x = ((int)r) % size;	
		return x >= 0 ? x : -x ; 
	}
	
	static public StringBuffer printBits(long out) {
		StringBuffer s = new StringBuffer();
	
		for(int k=0;k<65;k++) {
			s.append((out & 1)==1?"1":"0");
			out >>=1;
		}
		s.reverse();
		return s;
	}
	
	
	
	public  static void main(String args[]) {
		
		long l =123456;
		long l2 =1010119;
		System.out.println("l \t"+l+"\t"+printBits(l));

		long x =100000000;
		System.out.println("1m\t"+l2+"\t"+printBits(x)+"\t"+x);

		System.out.println("l2\t"+l2+"\t"+printBits(l));

		System.out.println("l2*l\t"+l2+"\t"+printBits(l*l2)+" \t "+l*l2);
		
		System.out.println("l2*l*l2\t"+l2+"\t"+printBits(l*l2*l2)+" \t "+l*l2*l2);
		
		System.out.println("l2*l*l2\t"+l2+"\t"+printBits(l*l2*l2*l2)+" \t "+l*l2*l2*l2);
		
		
		System.out.println("l2*l*l2\t"+l2+"\t"+printBits((l*l2)%0xfffff)+" \t "+l*l2*l2*l2+"\t "+0xfffff);
		System.out.println("l2*l*l2\t"+l2+"\t"+printBits((l*l2)&0xfffffff)+" \t "+l*l2*l2*l2);
		
		System.out.println(new Long2Int(353522).l2i(0l));
	}


	@Override
	public void freeze() {		
	}
	@Override
	public void unFreeze() {
	}

	@Override
	public boolean frozen() {
		return true;
	}
}
