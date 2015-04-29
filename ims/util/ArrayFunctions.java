package ims.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArrayFunctions {

	public static boolean same(int[] a,int[] b){
		if(a==null || b==null)
			return false;
		if(a.length!=b.length)
			return false;
		Arrays.sort(a);
		Arrays.sort(b);
		for(int i=0;i<a.length;++i)
			if(a[i]!=b[i])
				return false;
		return true;
	}
	
	public static int countSame(int[] i,int[] j){
		return countSame(i,j,0,i.length);
	}
	
	public static int countSame(int[] i, int[] j,int from,int until) {
		if(i==null || j==null)
			return 0;
		if(i.length!=j.length)
			throw new Error("errr");
		int same=0;
		for(int q=from;q<until;++q){
			if(i[q]==j[q])
				++same;
		}
		return same;
	}
	
	public static int countSame(float[] i,float[] j){
		return countSame(i,j,0,i.length);
	}
	
	public static int countSame(float[] i, float[] j,int from,int until) {
		if(i==null || j==null)
			return 0;
		if(i.length!=j.length)
			throw new Error("errr");
		int same=0;
		for(int q=from;q<until;++q){
			if(i[q]==j[q])
				++same;
		}
		return same;
	}
	
	public static <T> boolean sameTwice(T[] n){
		Set<T> seen=new HashSet<T>();
		for(T a:n){
			if(seen.contains(a))
				return true;
			seen.add(a);
		}
		return false;
	}
	
	public static <T> int countNonNull(T[] a){
		int cnt=0;
		for(T t:a)
			if(t!=null)
				++cnt;
		return cnt;
	}
	
	public static <T> List<T> concat(List<T>... lists){
		List<T> r=new ArrayList<T>();
		for(List<T> l:lists)
			r.addAll(l);
		return r;
	}
}
