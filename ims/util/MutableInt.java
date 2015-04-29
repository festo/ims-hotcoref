package ims.util;

public class MutableInt {
	int value;
	public MutableInt(){
		this(1);
	}
	public MutableInt(int value){
		this.value=value;
	}
	public void increment(){
		value++;
	}
	public int getValue(){
		return value;
	}
	public void setValue(int v){
		value=v;
	}
	public String toString(){
		return Integer.toString(value);
	}
}