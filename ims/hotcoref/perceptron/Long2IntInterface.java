package ims.hotcoref.perceptron;

import java.io.Serializable;

/*
 * This class is heavily based on is2.data.Long2IntInterface from mate-tools,
 * see http://code.google.com/p/mate-tools
 * 
 */
public interface Long2IntInterface extends Serializable {

	String ZIP_ENTRY = "_l2i";


	public abstract int size();


	/** 
	 * Maps a long to a integer value. This is very useful to save memory for sparse data long values 
	 * @param l
	 * @return the integer
	 */
	public abstract int l2i(long l);


	public abstract void freeze();
	public abstract void unFreeze();

	public abstract boolean frozen();
	

}